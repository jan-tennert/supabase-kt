pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

// Main Modules
include("GoTrue")
include("Postgrest")
include("Storage")
include("Realtime")
include("Functions")
include("bom")

// Test module
include("test-common")

// Serializers
include(":serializers:Moshi")
project(":serializers:Moshi").name = "serializer-moshi"
include(":serializers:Jackson")
project(":serializers:Jackson").name = "serializer-jackson"

// Misc plugins
include(":plugins:ApolloGraphQL")
include(":plugins:ComposeAuth")
include(":plugins:ComposeAuthUI")
include(":plugins:CoilIntegration")
include(":plugins:ImageLoaderIntegration")
include(":plugins:GenDatabaseTypes:gdp-ksp")
include(":plugins:GenDatabaseTypes:gdp-plugin")
include(":plugins:GenDatabaseTypes:gdp-core")

// Samples
if (System.getProperty("LibrariesOnly") != "true") {
    includeSample("chat-demo-mpp", "common", "web", "ios", "desktop", "android")
    includeSample("file-upload", "common", "desktop", "android")
}

// Renames
project(":GoTrue").name = "gotrue-kt"
project(":Postgrest").name = "postgrest-kt"
project(":Storage").name = "storage-kt"
project(":Realtime").name = "realtime-kt"
project(":Functions").name = "functions-kt"
project(":plugins:ApolloGraphQL").name = "apollo-graphql"
project(":plugins:ComposeAuth").name = "compose-auth"
project(":plugins:ComposeAuthUI").name = "compose-auth-ui"
project(":plugins:CoilIntegration").name = "coil-integration"
project(":plugins:ImageLoaderIntegration").name = "imageloader-integration"
rootProject.name = "supabase-kt"

fun includeSample(name: String, vararg targets: String) {
    targets.forEach {
        include(":sample:$name:$it")
    }
}
