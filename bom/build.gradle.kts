plugins {
    `java-platform`
}

description = "A Kotlin Multiplatform Supabase Framework"

val bomProject = project

val excludedModules = listOf("test-common")

fun shouldIncludeInBom(candidateProject: Project) =
    excludedModules.all { !candidateProject.name.contains(it) } &&
            candidateProject.name != bomProject.name

rootProject.subprojects.filter(::shouldIncludeInBom).forEach { bomProject.evaluationDependsOn(it.path) }

dependencies {
    constraints {
        rootProject.subprojects.filter { project ->
            // Only declare dependencies on projects that will have publications
            shouldIncludeInBom(project) && project.tasks.findByName("publish")?.enabled == true
        }.forEach { api(project(it.path)) }
    }
}
