@file:Suppress("UnusedImport")

package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*

open class LovelyPluginExtension(val project: Project) {

    fun gitProject() {
        project.gitProject()
    }

    val dockerFiles = project.copySpec()

    fun dockerProject(repository: String) {
        project.dockerProject(repository, dockerFiles)
    }
}

@Suppress("unused")
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

    @Suppress("unused")
    @TaskAction
    fun create() {
        val g = LSGit(project.rootProject.rootDir)
        val version = g.createVersionTag()
        println("Created and pushed tag $version")
    }
}

val GIT_GROUP = "Git"

fun Project.gitProject() {
    val g = LSGit(rootProject.rootDir)
    project.version = g.describe()

    tasks {
        val printVersion by creating {
            group = GIT_GROUP
            description = "Prints the current version of the Project to stdout"
            doLast { println(project.version) }
        }

        val createTag by creating(CreateTagTask::class) {
        }
    }
}

