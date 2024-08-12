@file:Suppress("unused")

package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

open class LovelyPluginExtension(private val project: Project) {

    fun gitProject() {
        project.gitProject()
    }

    fun pythonProject(pythonExecutable: String = "python3.8") {
        project.pythonProject(pythonExecutable)
    }

    fun dockerProject(
        repository: String,
        stages: List<String> = listOf(""),
        platforms: List<String> = listOf("linux/amd64", "linux/arm64"),
        buildPlatforms: List<String> = emptyList(),
        dockerFiles: Sync.() -> Unit = {}
    ) {
        project.dockerProject(repository, stages, platforms, buildPlatforms, dockerFiles)
    }

    fun awsProject(profile: String, ssoSessionSettings: SsoSessionSettings = SsoSessionSettings()) {
        project.awsProject(profile, ssoSessionSettings)
    }

}

class LovelyGradlePlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        extensions.create("lovely", LovelyPluginExtension::class.java, project)
    }
}

open class CreateTagTask : DefaultTask() {

    init {
        group = GIT_GROUP
        description = "Creates a new tag for the given version and pushes it upstream"
    }

    @TaskAction
    fun create() {
        val g = LSGit(project.rootProject.rootDir)
        val version = g.createVersionTag()
        println("Created and pushed tag ${version.second} with release date ${version.first}")
    }
}

const val GIT_GROUP = "Git"

fun Project.gitProject() {
    val g = LSGit(rootProject.rootDir)
    project.version = g.describe()

    tasks {
        val printVersion by registering {
            group = GIT_GROUP
            description = "Prints the current version of the Project to stdout"
            doLast { println(project.version) }
        }

        val printChangeLogVersion by registering {
            group = GIT_GROUP
            description = "Parses the changes file and prints out the latest defined version"
            doLast { println(g.parseChangeLog().latestVersion() ?: "unreleased") }
        }

        val createTag by registering(CreateTagTask::class) {
        }

    }
}
