package io.github.jan.supabase.ksp

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter

fun Sequence<KSAnnotation>.getAnnotationOrNull(target: String): KSAnnotation? {
    for (element in this) if (element.shortName.asString() == target) return element
    return null
}

fun <T> List<KSValueArgument>.getParameterValue(target: String): T {
    return getParameterValueIfExist(target) ?:
    throw NoSuchElementException("Sequence contains no element matching the predicate.")
}

fun <T> List<KSValueArgument>.getParameterValueIfExist(target: String): T? {
    for (element in this) if (element.name?.asString() == target) (element.value as? T)?.let { return it }
    return null
}

//Note these should never be null for our use case, but we still need to handle it
val KSValueParameter.nameAsString: String get() = name?.asString() ?: error("Parameter name is null")

val KSDeclaration.qualifiedNameAsString get() = qualifiedKSName.asString()
val KSDeclaration.qualifiedKSName get() = qualifiedName ?: error("Qualified name is null")
val KSDeclaration.simpleNameAsString get() = simpleName.asString()