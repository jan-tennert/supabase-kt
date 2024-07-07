plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.android.library.get().pluginId)
}

description = "KSP compiler for GenDatabaseTypes"

repositories {
    mavenCentral()
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    defaultConfig()
    /*applyDefaultHierarchyTemplate {
        common {
            group("noDefault") {
                withJvm()
                withJs()
            }
        }
    }*/
    allTargets()
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.ksp.processing.api)
                implementation(libs.kotlin.poet.ksp)
            }
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
        }
    }
}

configureLibraryAndroidTarget()
