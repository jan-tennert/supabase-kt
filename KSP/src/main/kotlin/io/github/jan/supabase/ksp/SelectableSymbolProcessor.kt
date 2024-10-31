package io.github.jan.supabase.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.annotations.ApplyFunction
import io.github.jan.supabase.postgrest.annotations.Cast
import io.github.jan.supabase.postgrest.annotations.ColumnName
import io.github.jan.supabase.postgrest.annotations.Foreign
import io.github.jan.supabase.postgrest.annotations.JsonPath
import io.github.jan.supabase.postgrest.annotations.Selectable

class SelectableSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    val packageName = options["selectablePackageName"] ?: "io.github.jan.supabase.postgrest"
    val fileName = options["selectableFileName"] ?: "PostgrestColumns"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Selectable::class.java.name).filterIsInstance<KSClassDeclaration>()
        if (!symbols.iterator().hasNext()) return emptyList()
        val types = hashMapOf<String, String>()
        symbols.forEach { symbol ->
            val className = symbol.simpleName.asString()
            val qualifiedName = symbol.simpleName.asString() //Kotlin JS doesn't support qualified names
            if (!symbol.modifiers.contains(Modifier.DATA)) {
                logger.error("The class $className is not a data class", symbol)
                return emptyList()
            }
            val parameters = symbol.primaryConstructor?.parameters
            if(parameters == null) {
                logger.error("Primary constructor is null or has no parameter", symbol)
                return emptyList()
            }
            val columns = parameters.map { processParameters(it, resolver) }.joinToString(",")
            types[qualifiedName] = columns
        }
        writePostgrestExtensionFunction(types, symbols.mapNotNull { it.containingFile }.toList())
        return emptyList()
    }

    private fun writePostgrestExtensionFunction(
        columns: Map<String, String>,
        sources: List<KSFile>
    ) {
        val function = FunSpec.builder("addSelectableTypes")
            .addKdoc(COMMENT)
            .addAnnotation(AnnotationSpec.builder(ClassName.bestGuess("kotlin.OptIn")).addMember("%T::class",
                SupabaseInternal::class).build())
            .receiver(Postgrest.Config::class)
        columns.forEach { (qualifiedName, columns) ->
            function.addStatement("columnRegistry.registerColumns(\"$qualifiedName\", \"$columns\")")
        }
        val fileSpec = FileSpec.builder(packageName, fileName)
            .addFunction(function.build())
            .addImport("io.github.jan.supabase.postgrest.annotations", "Selectable")
            .build()
        val stream = codeGenerator.createNewFile(
            Dependencies(false, *sources.toTypedArray()),
            packageName,
            fileName,
            extensionName = "kt"
        )
        stream.bufferedWriter().use {
            fileSpec.writeTo(it)
        }
    }

    private fun processParameters(parameter: KSValueParameter, resolver: Resolver): String {
        val parameterClass = resolver.getClassDeclarationByName(parameter.type.resolve().declaration.qualifiedKSName)
        requireNotNull(parameterClass) { "Parameter class is null" }
        val innerColumns = if(!isPrimitive(parameterClass.qualifiedNameAsString)) {
            val annotation = parameterClass.annotations.getAnnotationOrNull(Selectable::class.java.simpleName)
            if(annotation == null) {
                //Could be a JSON column or a custom type, so don't throw an error
                logger.info("Type of parameter ${parameter.nameAsString} is not a primitive type and does not have @Selectable annotation", parameter)
                ""
            } else {
                val columns = parameterClass.primaryConstructor?.parameters
                if(columns == null) {
                    logger.error("Primary constructor of ${parameterClass.qualifiedName} is null or has no parameter", parameter)
                    return ""
                }
                columns.map { processParameters(it, resolver) }.joinToString(",")
            }
        } else ""
        val alias = parameter.nameAsString
        val columnName = parameter.annotations.getAnnotationOrNull(ColumnName::class.java.simpleName)
            ?.arguments?.getParameterValue<String>(ColumnName.NAME_PARAMETER_NAME)
        val isForeign = parameter.annotations.getAnnotationOrNull(Foreign::class.java.simpleName) != null
        val jsonPathArguments = parameter.annotations.getAnnotationOrNull(JsonPath::class.java.simpleName)?.arguments
        val jsonPath = jsonPathArguments?.getParameterValue<ArrayList<String>>(JsonPath.PATH_PARAMETER_NAME)
        val returnAsText = jsonPathArguments?.getParameterValue<Boolean>(JsonPath.RETURN_AS_TEXT_PARAMETER_NAME) == true
        val function = parameter.annotations.getAnnotationOrNull(ApplyFunction::class.java.simpleName)
            ?.arguments?.getParameterValue<String>(ApplyFunction.FUNCTION_PARAMETER_NAME)
        val cast = parameter.annotations.getAnnotationOrNull(Cast::class.java.simpleName)
            ?.arguments?.getParameterValue<String>(Cast.TYPE_PARAMETER_NAME)
        val options = ColumnOptions(
            alias = alias,
            columnName = columnName,
            isForeign = isForeign,
            jsonPath = jsonPath,
            returnAsText = returnAsText,
            function = function,
            cast = cast,
            innerColumns = innerColumns
        )
        checkValidCombinations(
            parameterName = parameter.nameAsString,
            options = options,
            symbol = parameter
        )
        return buildColumns(
            options = options,
            parameterName = parameter.nameAsString,
            symbol = parameter,
            qualifiedTypeName = parameterClass.simpleName.asString() //Qualified names are not supported in Kotlin JS
        )
    }

    private fun checkValidCombinations(
        options: ColumnOptions,
        parameterName: String,
        symbol: KSNode
    ) {
        if(options.isForeign && options.jsonPath != null) {
            logger.error("Parameter $parameterName can't have both @Foreign and @JsonPath annotation", symbol)
        }
        if(options.isForeign && options.function != null) {
            logger.error("Parameter $parameterName can't have both @Foreign and @ApplyFunction annotation", symbol)
        }
        if(options.jsonPath != null && options.function != null) {
            logger.error("Parameter $parameterName can't have both @JsonPath and @ApplyFunction annotation", symbol)
        }
        if(options.jsonPath != null && options.columnName == null) {
            logger.error("Parameter $parameterName must have a @ColumnName annotation when using @JsonPath", symbol)
        }
        if(options.jsonPath != null && options.jsonPath.isEmpty()) {
            logger.error("Parameter $parameterName must have at least one path in @JsonPath annotation", symbol)
        }
    }

    private fun buildColumns(
        options: ColumnOptions,
        parameterName: String,
        qualifiedTypeName: String,
        symbol: KSNode
    ): String {
        return buildString {
            if(options.jsonPath != null) {
                if(options.alias != options.jsonPath.last()) {
                    append("${options.alias}:")
                }
                append(buildJsonPath(options.columnName, options.jsonPath, options.returnAsText))
                return@buildString
            }

            //If the column name is not provided, use the alias (parameter name)
            if(options.columnName == null) {
                append(options.alias)
            } else {
                append("${options.alias}:${options.columnName}")
            }
            if(options.isForeign) {
                append("(${options.innerColumns})")
                return@buildString
            }

            //If a custom cast is provided, use it
            if(options.cast != null && options.cast.isNotEmpty()) {
                append("::${options.cast}")
            } else if(options.cast != null) { //If cast is empty, try to auto-cast
                val autoCast = primitiveColumnTypes[qualifiedTypeName]
                if(autoCast != null) {
                    append("::$autoCast")
                } else {
                    logger.error("Type of parameter $parameterName is not a primitive type and does not have an automatic cast type. Try to specify it manually.", symbol)
                }
            }
            if(options.function != null) {
                append(".${options.function}()")
            }
        }
    }

    private fun buildJsonPath(
        columnName: String?,
        jsonPath: List<String>,
        returnAsText: Boolean
    ): String {
        val operator = if(returnAsText) "->>" else "->"
        return buildString {
            append(columnName)
            if(jsonPath.size > 1) {
                jsonPath.dropLast(1).forEach {
                    append("->$it")
                }
            }
            append(operator)
            append(jsonPath.last())
        }
    }

    companion object {

        val COMMENT = """
            |Adds the types annotated with [Selectable] to the ColumnRegistry. Allows to use the automatically generated columns in the PostgrestQueryBuilder.
            |
            |This file is generated by the SelectableSymbolProcessor.
            |Do not modify it manually.
        """.trimMargin()

    }

}