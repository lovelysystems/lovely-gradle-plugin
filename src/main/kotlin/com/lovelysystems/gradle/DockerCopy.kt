package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

@Suppress("unused")
abstract class DockerCopy : DefaultTask() {

    @get:Input
    abstract val image: Property<String>

    @get:Input
    abstract val srcPath: Property<String>

    @get:Input
    @get:Optional
    abstract val platform: Property<String>

    @TaskAction
    fun copy() {
        val file = outputs.files.first()
        val containerId: String = ByteArrayOutputStream().use { os ->
            this.project.exec {
                if (platform.isPresent) {
                    val platformStr = platform.get().let { "--platform=$it" }
                    commandLine("docker", "create", platformStr, image.get())
                } else {
                    commandLine("docker", "create", image.get())
                }
                standardOutput = os
            }
            os.toString().trim()
        }
        this.project.exec {
            commandLine("docker", "cp", "${containerId}:${srcPath.get()}", file)
        }
        this.project.exec {
            commandLine("docker", "rm", containerId)
        }
    }
}
