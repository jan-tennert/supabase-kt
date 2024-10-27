import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.kotlin.poet)
    implementation(libs.kotlin.poet.ksp)
    implementation(project(":postgrest-kt"))
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.freeCompilerArgs.add("-opt-in=io.github.jan.supabase.annotations.SupabaseInternal")
}