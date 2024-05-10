plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.multiplatform.gradle)
    implementation(libs.android.gradle.plugin)
    implementation(libs.detekt.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.publishing.gradle)
}