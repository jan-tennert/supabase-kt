# Supabase KSP compiler

Currently only supports generating columns for `@Selectable` data classes.

To install it, add the KSP Gradle plugin to your project:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" //kotlinVersion-kspVersion
}
```

Then add the Supabase KSP compiler to your dependencies:

[**JVM**](https://kotlinlang.org/docs/ksp-quickstart.html#add-a-processor):

```kotlin
depdencies {
    ksp("io.github.jan-tennert.supabase:ksp-compiler:VERSION")
}
```

[**Multiplatform**](https://kotlinlang.org/docs/ksp-multiplatform.html):

```kotlin
kotlin {
    //...
    jvm()
    androidTarget()
    iosX64()
    //...
    
    //Might need to add this if you cannot see generated code in your IDE
    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

dependencies {
    //Add KSP compiler to all targets that need processing
    //Advised to use Gradle Version Catalogs
    add("kspCommonMainMetadata", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
    
    //Note: If only the common target needs processing, you don't need the following lines
    add("kspJvm", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
    add("kspAndroid", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
    add("kspIosX64", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
}

//Might need to add this if you cannot see generated code in your IDE
project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    if(name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
```