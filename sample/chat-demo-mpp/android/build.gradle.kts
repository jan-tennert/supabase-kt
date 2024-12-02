plugins {
    id(libs.plugins.compose.plugin.get().pluginId)
    alias(libs.plugins.compose.compiler)
    id("com.android.application")
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.kotlinx.plugin.serialization)
}

group = "io.supabase"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":sample:chat-demo-mpp:common"))
    implementation(libs.androidx.activity.compose)
}

android {
    configureApplicationAndroidTarget()
    kotlinOptions {
        jvmTarget = "1.8"
    }
}