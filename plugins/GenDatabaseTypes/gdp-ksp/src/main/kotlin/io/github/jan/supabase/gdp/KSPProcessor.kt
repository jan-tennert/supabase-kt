package io.github.jan.supabase.gdp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

class KSPProcessor constructor(private val environment: SymbolProcessorEnvironment): SymbolProcessor {
    private val logger: KSPLogger = environment.logger
    private val codeGenerator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val dbClass = resolver.getSymbolsWithAnnotation(SupabaseDB::class.java.name)
            .filterIsInstance<KSClassDeclaration>().firstOrNull() ?: return emptyList()
        val typeBuilder = TypeSpec.classBuilder("ProductTable")
        val fileSpec = FileSpec.builder(dbClass.packageName.asString(), "ProductTable")
            .addType(typeBuilder.build())
            .build()
        codeGenerator.createNewFile(
            Dependencies(false, dbClass.containingFile!!),
            fileSpec.packageName,
            fileSpec.name
        ).writer()
            .use { fileSpec.writeTo(it) }

        return emptyList()
    }

}