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