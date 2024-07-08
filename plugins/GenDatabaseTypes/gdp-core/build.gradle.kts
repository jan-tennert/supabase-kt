plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

description = "KSP compiler for GenDatabaseTypes"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization)
}