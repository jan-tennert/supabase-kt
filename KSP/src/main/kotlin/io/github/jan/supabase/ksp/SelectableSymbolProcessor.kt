package io.github.jan.supabase.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.jan.supabase.postgrest.annotations.ApplyFunction
import io.github.jan.supabase.postgrest.annotations.Cast
import io.github.jan.supabase.postgrest.annotations.ColumnName
import io.github.jan.supabase.postgrest.annotations.Foreign
import io.github.jan.supabase.postgrest.annotations.JsonPath
import io.github.jan.supabase.postgrest.annotations.Selectable
import io.github.jan.supabase.postgrest.query.Columns

class SelectableSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Selectable::class.java.name).filterIsInstance<KSClassDeclaration>()
        if (!symbols.iterator().hasNext()) return emptyList()
        symbols.forEach { symbol ->
            val className = symbol.simpleName.asString()
            val packageName = symbol.containingFile?.packageName?.asString().orEmpty()
            if (!symbol.modifiers.containsIgnoreCase("data")) {
                logger.error("This object is not a data class", symbol)
                return emptyList()
            }
            val companionObject = symbol.anyCompanionObject()
            if(companionObject == null) {
                logger.error("Companion object not found", symbol)
                return emptyList()
            }
            val parameters = symbol.primaryConstructor?.parameters
            if(parameters == null) {
                logger.error("Primary constructor is null or has no parameter", symbol)
                return emptyList()
            }
            val columns = parameters.map { processParameters(it, resolver) }.joinToString(",")
            writeColumnsExtensionProperty(symbol, companionObject, columns, "${className}Columns", packageName)
        }
        return emptyList()
    }

    private fun writeColumnsExtensionProperty(
        symbol: KSClassDeclaration,
        companionObject: KSClassDeclaration,
        columns: String,
        className: String,
        packageName: String
    ) {
        val fileSpec = FileSpec.builder(packageName, className)
            .addImport("io.github.jan.supabase.postgrest.query", "Columns")
            .addProperty(PropertySpec.builder("columns", Columns::class)
                .receiver(companionObject.toClassName())
                .getter(FunSpec.getterBuilder().addStatement("return Columns.raw(\"$columns\")").build())
                .build()
            )
            .build()
        codeGenerator.createNewFile(
            Dependencies(false, symbol.containingFile!!),
            packageName,
            className,
            extensionName = "kt"
        ).bufferedWriter().use {
            fileSpec.writeTo(it)
        }
    }

    private fun processParameters(parameter: KSValueParameter, resolver: Resolver): String {
        val parameterClass = resolver.getClassDeclarationByName(parameter.type.resolve().declaration.qualifiedName!!)
        val innerColumns = if(!isPrimitive(parameterClass!!.qualifiedName!!.asString())) {
            val annotation = parameterClass.annotations.getAnnotationOrNull(Selectable::class.java.simpleName)
            if(annotation == null) {
                logger.error("Type of parameter ${parameter.name!!.getShortName()} is not a primitive type and does not have @Selectable annotation", parameter)
                return ""
            } else {
                val columns = parameterClass.primaryConstructor?.parameters
                if(columns == null) {
                    logger.error("Primary constructor of ${parameterClass.qualifiedName} is null or has no parameter", parameter)
                    return ""
                }
                columns.map { processParameters(it, resolver) }.joinToString(",")
            }
        } else ""
        val alias = parameter.name!!.getShortName()
        val columnName = parameter.annotations.getAnnotationOrNull(ColumnName::class.java.simpleName)
            ?.arguments?.getParameterValue<String>(ColumnName.NAME_PARAMETER_NAME) ?: alias
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
            parameterName = parameter.name!!.asString(),
            options = options,
            symbol = parameter
        )
        return buildColumns(
            options = options,
            parameterName = parameter.name!!.asString(),
            symbol = parameter,
            qualifiedTypeName = parameterClass.qualifiedName!!.asString()
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
        if(options.jsonPath != null && options.jsonPath.isEmpty()) {
            logger.error("Parameter $parameterName can't have an empty @JsonPath annotation. At least two elements (the column name and a key) are required.", symbol)
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
                append(buildJsonPath(options.jsonPath, options.returnAsText))
                return@buildString
            }

            //If the alias is the same as the column name, we can just assume parameter name (alias) is the column name
            if(options.alias == options.columnName) {
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

    private fun buildJsonPath(jsonPath: List<String>, returnAsText: Boolean): String {
        val operator = if(returnAsText) "->>" else "->"
        val formattedPath = if(jsonPath.size > 1) jsonPath.dropLast(1).joinToString("->") else ""
        val key = jsonPath.last()
        return "$formattedPath$operator$key"
    }

}