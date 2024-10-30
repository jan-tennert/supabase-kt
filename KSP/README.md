# Supabase KSP compiler

Currently only supports generating columns for `@Selectable` types.

To install it, add the KSP Gradle plugin to your project:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" //kotlinVersion-kspVersion
}
```

Then add the Supabase KSP compiler to your dependencies:

> [!NOTE]
> `VERSION` is the same as the Supabase-kt version.

**JVM**:

```kotlin
depdencies {
    ksp("io.github.jan-tennert.supabase:ksp-compiler:VERSION")
}
```

**Multiplatform**:

```kotlin
kotlin {
    //...
    jvm()
    androidTarget()
    iosX64()
    //...
}

dependencies {
    //Advised to use Gradle Version Catalogs
    add("kspCommonMainMetadata", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
    add("kspJvm", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
    add("kspAndroid", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
    add("kspIosX64", "io.github.jan-tennert.supabase:ksp-compiler:VERSION")
}