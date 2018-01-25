package com.lovelysystems.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.*
import java.io.ByteArrayOutputStream

val DOCKER_GROUP = "Docker"

private fun Project.dockerTag(
    versionTag: String = version.toString(),
    registry: String? = null
): String {
    val prefix = if (registry != null) {
        "$registry/"
    } else {
        ""
    }
    return "$prefix${project.group}/${project.name}:${versionTag}"
}

private fun Project.validateVersion() {
    val tag = version.toString()
    if (!isProductionVersion(tag)) return
    val g = LSGit(project.rootProject.rootDir)
    g.validateProductionTag(tag)
}

fun Project.dockerProject(registry: String = "") {
    val distTar by tasks
    plugins.apply(ApplicationPlugin::class.java)
    tasks {

        val prepareDockerImage by creating(Sync::class) {
            description = "Prepares all files required for a Docker build"
            group = DOCKER_GROUP
            from(distTar.outputs.files)
            from(file("Dockerfile"))
            destinationDir = project.buildDir.resolve("docker")
        }

        val buildDockerDevImage by creating(Exec::class) {
            dependsOn(prepareDockerImage)
            group = DOCKER_GROUP
            description = "Builds a docker image and tags it with version and dev"
            inputs.files(prepareDockerImage.outputs)
            workingDir = file(prepareDockerImage.destinationDir)

            commandLine(
                "docker", "build", ".",
                "-t", project.dockerTag(),
                "-t", project.dockerTag("dev")
            )
        }

        val pushDockerDevImage by creating() {
            dependsOn(buildDockerDevImage)
            group = DOCKER_GROUP
            description = "Pushes the docker image to the registry"

            val tag = dockerTag(registry = registry)
            val devTag = dockerTag("dev", registry = registry)
            inputs.files("Dockerfile")

            if (!tag.endsWith(".dirty")) {
                doFirst {
                    val eo = ByteArrayOutputStream()
                    val e = exec {
                        isIgnoreExitValue = true
                        commandLine("docker", "pull", tag)
                        errorOutput = eo

                    }
                    if (e.exitValue == 0) {
                        throw RuntimeException("tag already exists $tag")
                    }
                }
            }

            doFirst {
                validateVersion()
            }

            doLast {
                exec {
                    commandLine("docker", "tag", dockerTag(), tag)
                }
                exec {
                    commandLine("docker", "tag", tag, devTag)
                }
                exec {
                    commandLine("docker", "push", tag)
                }
                exec {
                    commandLine("docker", "push", devTag)
                }
            }
        }
    }
}