plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.android.library.get().pluginId)
}

description = "Extends supabase-kt with a Storage Client"

repositories {
    mavenCentral()
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    defaultConfig()
    applyDefaultHierarchyTemplate {
        common {
            androidAndJvmGroup()
            settingsGroup()
        }
    }
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                addModules(SupabaseModule.GOTRUE)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":test-common"))
                implementation(libs.bundles.testing)
                implementation(libs.turbine)
            }
        }
        val settingsMain by getting {
            dependencies {
                api(libs.bundles.multiplatform.settings)
            }
        }
    }
}

configureLibraryAndroidTarget()