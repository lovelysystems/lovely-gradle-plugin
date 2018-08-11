package com.lovelysystems.gradle

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import java.io.ByteArrayOutputStream

const val DOCKER_GROUP = "Docker"

private fun Project.validateVersion() {
    val tag = version.toString()
    if (!isProductionVersion(tag)) return
    val g = LSGit(project.rootProject.rootDir)
    g.validateProductionTag(tag)
}

fun Project.dockerProject(repository: String, files: CopySpec) {

    val dockerBuildDir = project.buildDir.resolve("docker")

    fun Project.dockerTag(versionTag: String = version.toString()): String {
        return "$repository:${versionTag}"
    }

    tasks {

        @Suppress("UNUSED_VARIABLE")
        val printDockerTag by creating {
            description = "Prints out the computed docker tag for this project"
            group = DOCKER_GROUP
            doLast {
                println(dockerTag())
            }
        }

        val prepareDockerImage by creating(Sync::class) {
            description = "Prepares all files required for a Docker build"
            group = DOCKER_GROUP
            destinationDir = dockerBuildDir
        }

        afterEvaluate {
            with(prepareDockerImage) {
                with(files)
                from(file("Dockerfile"))
            }
        }

        val buildDockerImage by creating(Exec::class) {
            dependsOn(prepareDockerImage)
            group = DOCKER_GROUP
            description = "Builds a docker image and tags it with version and dev"
            inputs.files(prepareDockerImage.outputs)
            workingDir = dockerBuildDir

            commandLine(
                "docker", "build", ".",
                "-t", project.dockerTag(),
                "-t", project.dockerTag("dev")
            )
        }

        val pushDockerImage by creating {
            dependsOn(buildDockerImage)
            group = DOCKER_GROUP
            description = "Pushes the docker image to the registry"
            val tag = dockerTag()
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
                    commandLine("docker", "push", tag)
                }
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val pushDockerDevImage by creating {
            dependsOn(pushDockerImage)
            group = DOCKER_GROUP
            description = "Pushes the docker image to the registry and tag it as dev"
            val devTag = dockerTag("dev")
            doLast {
                exec {
                    commandLine("docker", "push", devTag)
                }
            }
        }
    }
}