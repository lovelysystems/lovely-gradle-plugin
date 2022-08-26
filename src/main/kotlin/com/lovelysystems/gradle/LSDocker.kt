package com.lovelysystems.gradle

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
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

fun Project.dockerProject(repository: String, files: CopySpec, stages: List<String>, platforms: List<String>) {

    val dockerBuildDir = project.buildDir.resolve("docker")

    fun Project.dockerTag(versionTag: String = version.toString(), stage: String = ""): String {
        if (stage.isEmpty()) {
            return "$repository:${versionTag}"
        } else {
            return "$repository:${versionTag}-$stage"
        }
    }

    tasks {

        val multiplatformBuilder = "lovely-docker-container-builder"

        fun buildXCmd(vararg tags: String, push: Boolean = false): Array<String> {
            val imageTags = tags.map { listOf("-t", it) }.flatten().toTypedArray()
            val buildArgs = if (push) {
                arrayOf(
                    "--platform", platforms.joinToString(","),
                    "--push"
                )
            } else {
                // local builds do not require a foreign target platform
                arrayOf("--load")  // ensures build image is registered in local docker registry
            }
            return arrayOf(
                "docker", "buildx",
                "build", ".",
                // always use the builder using the docker-container driver in order to access BuildKit features
                "--builder", multiplatformBuilder,
                *imageTags,
                *buildArgs,
            )
        }

        @Suppress("UNUSED_VARIABLE")
        val printDockerTag by creating {
            description = "Prints out the computed docker tag(s) for this project"
            group = DOCKER_GROUP
            doLast {
                stages.forEach {
                    println(dockerTag(stage = it))
                }
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

        /**
         * Bootstraps a new docker-container builder
         */
        @Suppress("UNUSED_VARIABLE")
        val prepareDockerContainerBuilder by creating {
            doLast {
                exec {
                    // The node will also automatically detect the platforms it supports
                    commandLine(
                        "docker", "buildx",
                        "create",
                        "--name", multiplatformBuilder,
                        // Uses a BuildKit container that will be spawned via docker.
                        // With this driver, both building multi-platform images and exporting cache are supported.
                        "--driver", "docker-container",
                        "--bootstrap"
                    )
                }
            }
        }

        val buildDockerImage by creating {
            dependsOn(prepareDockerImage)
            group = DOCKER_GROUP
            description = "Builds a docker image and tags it with version and dev"
            inputs.files(prepareDockerImage.outputs)

            doLast {
                stages.forEach { stage ->
                    logger.info("building docker image stage=$stage")
                    exec {
                        val cmd = buildXCmd(project.dockerTag(stage = stage), project.dockerTag("dev", stage))
                        workingDir = dockerBuildDir
                        commandLine(*cmd)
                        if (stage.isNotEmpty()) {
                            args("--target", stage)
                        }
                    }
                    logger.info("docker build finished for stage=$stage")
                }
            }
        }

        val pushDockerImage by creating {
            dependsOn(buildDockerImage)
            group = DOCKER_GROUP
            description = "Pushes the docker image to the registry"
            inputs.files("Dockerfile")
            val versionString = version.toString()

            doFirst {
                validateVersion()
            }


            if (!versionString.endsWith(".dirty")) {
                stages.forEach { stage ->
                    val tag = dockerTag(stage = stage)
                    doFirst {
                        val eo = ByteArrayOutputStream()
                        val e = exec {
                            isIgnoreExitValue = true
                            commandLine("docker", "pull", tag)
                            errorOutput = eo

                        }
                        val shouldPushDev = project.gradle.taskGraph.hasTask(":pushDockerDevImage")
                        if (e.exitValue == 0 && !shouldPushDev) {
                            throw RuntimeException("tag already exists $tag")
                        }
                    }
                    doLast {
                        exec {
                            val cmd = buildXCmd(tag, push = true)
                            workingDir = dockerBuildDir
                            commandLine(*cmd)
                        }
                    }

                }
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val pushDockerDevImage by creating {
            dependsOn(pushDockerImage)
            group = DOCKER_GROUP
            description = "Pushes the docker image to the registry and tag it as dev"
            stages.forEach { stage ->
                val devTag = dockerTag("dev", stage = stage)
                doLast {
                    exec {
                        val cmd = buildXCmd(devTag, push = true)
                        workingDir = dockerBuildDir
                        commandLine(*cmd)
                    }
                }

            }
        }
    }
}
