plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.android.library.get().pluginId)
}

description = "Extends supabase-kt with a Apollo GraphQL Client"

repositories {
    mavenCentral()
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    defaultConfig()
    jvmTargets()
    jsTarget()
    iosTargets()
    macosTargets()
    watchosArm64()
    watchosSimulatorArm64()
    tvosTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":"))
                api(project(":gotrue-kt"))
                api(libs.apollo.kotlin)
            }
        }
    }
}

configureAndroidTarget()