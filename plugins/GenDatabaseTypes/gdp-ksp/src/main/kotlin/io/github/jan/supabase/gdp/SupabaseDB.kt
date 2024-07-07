package io.github.jan.supabase.gdp

@Retention(AnnotationRetention.SOURCE) // we only need this in source files

@Target(AnnotationTarget.CLASS) // should only go on classes or interfaces
annotation class SupabaseDB
