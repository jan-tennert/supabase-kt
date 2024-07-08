plugins {
    `kotlin-dsl`
}

description = "KSP compiler for GenDatabaseTypes"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("databaseCodeGenPlugin") {
            id = "io.github.jan.supabase.gdp.plugin"
            implementationClass = "io.github.jan.supabase.gdp.GenDatabaseTypesPlugin"
        }
    }
}
