
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
include("Auth")
include("Postgrest")
include("KSP")
include("Storage")
include("Realtime")
include("Functions")
include("Supabase")
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
include(":plugins:Coil3Integration")
include(":plugins:CoilIntegration")
include(":plugins:ImageLoaderIntegration")

// Samples
if (System.getProperty("LibrariesOnly") != "true") {
    includeSample("chat-demo-mpp", "common", "web", "ios", "desktop", "android")
    includeSample("file-upload", "common", "desktop", "android")
    includeSample("multi-factor-auth", "common", "desktop", "android", "web")
}

// Renames
project(":Auth").name = "auth-kt"
project(":Postgrest").name = "postgrest-kt"
project(":Storage").name = "storage-kt"
project(":Realtime").name = "realtime-kt"
project(":Functions").name = "functions-kt"
project(":Supabase").name = "supabase-kt"
project(":KSP").name = "ksp-compiler"
project(":plugins:ApolloGraphQL").name = "apollo-graphql"
project(":plugins:ComposeAuth").name = "compose-auth"
project(":plugins:ComposeAuthUI").name = "compose-auth-ui"
project(":plugins:Coil3Integration").name = "coil3-integration"
project(":plugins:CoilIntegration").name = "coil-integration"
project(":plugins:ImageLoaderIntegration").name = "imageloader-integration"
rootProject.name = "supabase-kt"

fun includeSample(name: String, vararg targets: String) {
    targets.forEach {
        include(":sample:$name:$it")
    }
}
