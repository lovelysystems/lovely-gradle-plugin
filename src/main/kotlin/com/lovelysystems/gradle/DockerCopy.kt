package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

abstract class DockerCopy : DefaultTask() {

    @get:Input
    abstract val image: Property<String>

    @get:Input
    abstract val srcPath: Property<String>

    @TaskAction
    fun copy() {
        val file = outputs.files.first()
        var containerId: String = ByteArrayOutputStream().use { os ->
            this.project.exec {
                commandLine("docker", "create", image.get())
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