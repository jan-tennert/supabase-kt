# Supabase-kt Postgrest

Extends Supabase-kt with a multiplatform Postgrest client.

Supported targets:

| Target | **JVM** | **Android** | **JS** | **Wasm** | **Apple** | **Windows** | **Linux** |
|--------|---------|-------------|--------|----------|-----------|-------------|-----------|
| Status | ✅       | ✅           | ✅      | ✅        | ✅         | ✅           | ✅         |

<details>

<summary>In-depth Kotlin targets</summary>

**JS**: Browser, NodeJS

**Wasm**: wasm-js

**Apple:**

- iOS: iosArm64, iosSimulatorArm64, iosX64

- tvOS: tvosArm64, tvosX64, tvosSimulatorArm64

- watchOS: watchosArm64, watchosX64, watchosSimulatorArm64

- MacOS: macosX64, macosArm64

**Windows**: mingwX64

**Linux**: linuxX64

</details>

# Installation

Newest version: [![](https://img.shields.io/github/release/supabase-community/supabase-kt?label=)](https://github.com/supabase-community/supabase-kt/releases)

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supabase:postgrest-kt:VERSION")
}
```

Install the plugin in your SupabaseClient. See the [documentation](https://supabase.com/docs/reference/kotlin/initializing) for more information

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "https://id.supabase.co",
    supabaseKey = "apikey"
) {

    //...

    install(Postgrest) {
        // settings
    }

}
```

> [!NOTE]
> Generating columns for `@Selectable` data classes requires the [KSP compiler](/KSP) to be installed.

# Usage

See [Postgrest documentation](https://supabase.com/docs/reference/kotlin/select) for usage
