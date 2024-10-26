package io.github.jan.supabase.ksp

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Modifier

fun Sequence<KSAnnotation>.getAnnotation(target: String): KSAnnotation {
    return getAnnotationOrNull(target) ?:
    throw NoSuchElementException("Sequence contains no element matching the predicate.")
}

fun KSClassDeclaration.anyCompanionObject(): KSClassDeclaration? {
    return declarations.filterIsInstance<KSClassDeclaration>().firstOrNull { it.isCompanionObject }
}

fun Sequence<KSAnnotation>.getAnnotationOrNull(target: String): KSAnnotation? {
    for (element in this) if (element.shortName.asString() == target) return element
    return null
}

fun Sequence<KSAnnotation>.hasAnnotation(target: String): Boolean {
    for (element in this) if (element.shortName.asString() == target) return true
    return false
}

fun <T> List<KSValueArgument>.getParameterValue(target: String): T {
    return getParameterValueIfExist(target) ?:
    throw NoSuchElementException("Sequence contains no element matching the predicate.")
}

fun <T> List<KSValueArgument>.getParameterValueIfExist(target: String): T? {
    for (element in this) if (element.name?.asString() == target) (element.value as? T)?.let { return it }
    return null
}

fun Collection<Modifier>.containsIgnoreCase(name: String): Boolean {
    return stream().anyMatch { it.name.equals(name, true) }
}