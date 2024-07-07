# Supabase-kt Compose Auth

Extends gotrue-kt with Native Auth composables for Compose Multiplatform

Supported targets:

| Target | **JVM** | **Android** | **JS** | **iOS** | **tvOS** | **watchOS** | **macOS** | **Windows** | **Linux** |
| ------ | ------- | ----------- | ------ | ------- | -------- | ----------- | --------- | ----------- | --------- |
|        | ☑️      | ✅          | ☑️     | ✅      | ❌       | ❌          | ❌        | ❌          | ❌        |

> Note: iOS support is experimental and needs feedback
>
> ☑️ = Has no support for neither Google nor Apple Native Auth, relies on gotrue-kt for OAuth.

<details>

<summary>In-depth Kotlin targets</summary>

**iOS:** iosArm64, iosSimulatorArm64, iosX64

**JS**: Browser, NodeJS

**tvOS**: tvosArm64, tvosX64, tvosSimulatorArm64

**watchOS**: watchosArm64, watchosX64, watchosSimulatorArm64

**MacOS**: macosX64, macosArm64

**Windows**: mingwX64

**Linux**: linuxX64

</details>

# Installation

Newest version: [![](https://img.shields.io/github/release/supabase-community/supabase-kt?label=)](https://github.com/supabase-community/supabase-kt/releases)

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supabase:compose-auth:VERSION")
}
```

Install the plugin in your SupabaseClient. See the [documentation](https://supabase.com/docs/reference/kotlin/initializing) for more information

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "https://id.supabase.co",
    supabaseKey = "apikey"
) {
    //...
    install(Auth) {
        //your config
    }
    install(ComposeAuth) {
        googleNativeLogin(serverClientId = "google-client-id")
        appleNativeLogin()
    }
}
```

# Native Auth Support

Currently, Compose Auth only supports Native Auth for
Android with Google (via the Credential Manager) and iOS with Apple, other variations such as JS and JVM rely on fallback which
by default is GoTrue-kt OAuth flow.

To learn how you can use this plugin in your compose project, visit [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#readme)

# Usage

The composable can be accessed trough `composeAuth` property from `supabase`

```kotlin
val action = supabase.composeAuth.rememberSignInWithGoogle(
    onResult = { result -> //optional error handling
        when (result) {
            is NativeSignInResult.Success -> {}
            is NativeSignInResult.ClosedByUser -> {}
            is NativeSignInResult.Error -> {}
            is NativeSignInResult.NetworkError -> {}
        }
    },
    fallback = { // optional: add custom error handling, not required by default

    }
)

Button(
    onClick = { action.startFlow() } //optional: you can also pass in extra data for the user like a name. A nonce is automatically generated, but you can also pass in a custom nonce 
) {
    Text("Google Login")
}
```

# Native Google Auth on Android

Here is a small guide on how to use Native Google Auth on Android:

1. Create a project in your [Google Cloud Developer Console](https://console.cloud.google.com/)
2. Create OAuth credentials for a Web application, and use your Supabase callback url as redirect url. (**https://ID.supabase.co/auth/v1/callback**)
3. Put in the Web OAuth in your Supabase Auth Settings for Google in the Dashboard
4. Create OAuth credentials for an Android app, and put in your package name and SHA-1 certificate (which you can get by using `gradlew signingReport`)
5. Put the Android OAuth client id to the authorized client ids in the Supabase Dashboard
6. Use the **Web** OAuth client id in the Compose Auth plugin
