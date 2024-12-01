import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

enum class SupabaseModule(val module: String) {
    SUPABASE("supabase-kt"),
    AUTH("auth-kt"),
    STORAGE("storage-kt"),
    REALTIME("realtime-kt"),
    FUNCTIONS("functions-kt"),
    POSTGREST("postgrest-kt"),
    COMPOSE_AUTH("plugins:compose-auth"),
    COMPOSE_AUTH_UI("plugins:compose-auth-ui"),
}

fun KotlinDependencyHandler.addModules(vararg modules: SupabaseModule) {
    modules.forEach {
        api(project(":${it.module}"))
    }
}

fun DesktopExtension.configureComposeDesktop(
    name: String,
) {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = name
            packageVersion = "1.0.0"
        }
    }
}