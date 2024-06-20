package com.lovelysystems.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import java.io.ByteArrayOutputStream

const val DOCKER_GROUP = "Docker"

private fun Project.validateVersion() {
    val tag = version.toString()
    if (!isProductionVersion(tag)) return
    val g = LSGit(project.rootProject.rootDir)
    g.validateProductionTag(tag)
}

/**
 * Holds the tasks and settings to build and push docker images with [BuildKit](https://docs.docker.com/develop/develop-images/build_enhancements/).
 *
 * @param repository: The docker repository name e.g. "hub.example.com/lovely/exampleproject"
 * @param stages: to build multiple stages from a single Dockerfile
 * @param platforms: the target platforms (system architecture) for the build
 * @param buildPlatforms: the build platforms used for local docker build.
 * @param dockerFiles: a specification for copied docker-files
 */
fun Project.dockerProject(
    repository: String,
    stages: List<String>,
    platforms: List<String>,
    buildPlatforms: List<String>,
    dockerFiles: Sync.() -> Unit,
) {
    if (platforms.isEmpty()) {
        error("List of platforms is empty.")
    }

    val dockerBuildDir = project.layout.buildDirectory.get().asFile.resolve("docker")

    fun Project.dockerTag(versionTag: String = version.toString(), stage: String = ""): String {
        return if (stage.isEmpty()) {
            "$repository:${versionTag}"
        } else {
            "$repository:${versionTag}-$stage"
        }
    }

    tasks {

        val multiplatformBuilder = "lovely-docker-container-builder"

        fun buildXCmd(vararg tags: String, push: Boolean = false, stage: String = ""): Array<String> {
            val args = mutableListOf(
                "docker", "buildx",
                "build", ".",
                // always use the builder using the docker-container driver in order to access BuildKit features
                "--builder", multiplatformBuilder,
            )
            args.addAll(tags.map { listOf("-t", it) }.flatten())

            if (push) {
                args.add("--push")
                platforms
            } else {
                args.add("--load")
                buildPlatforms
            }.takeIf { it.isNotEmpty() }?.let {
                args.add("--platform")
                args.add(platforms.joinToString(","))
            }

            if (stage.isNotEmpty()) {
                args.add("--target")
                args.add(stage)
            }
            return args.toTypedArray()
        }

        val printDockerTag by registering {
            description = "Prints out the computed docker tag(s) for this project"
            group = DOCKER_GROUP
            doLast {
                stages.forEach {
                    println(dockerTag(stage = it))
                }
            }
        }

        val prepareDockerImage by registering(Sync::class) {
            description = "Prepares all files required for a Docker build"
            group = DOCKER_GROUP
            destinationDir = dockerBuildDir
            from(file("Dockerfile")) // by default take "Dockerfile" from root
            dockerFiles()
        }

        /**
         * Bootstraps a new docker-container builder
         */
        val prepareDockerContainerBuilder by registering {
            description = "Bootstraps a new docker-container builder"
            group = DOCKER_GROUP
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
                    )
                }
            }
        }

        val buildDockerImage by registering {
            dependsOn(prepareDockerImage)
            group = DOCKER_GROUP
            description = "Builds a docker image and tags it with version and dev"
            inputs.files(prepareDockerImage.get().outputs)

            doLast {
                stages.forEach { stage ->
                    logger.info("building docker image stage=$stage")
                    exec {
                        val cmd = buildXCmd(project.dockerTag(stage = stage), project.dockerTag("dev", stage), stage=stage)
                        workingDir = dockerBuildDir
                        commandLine(*cmd)
                    }
                    logger.info("docker build finished for stage=$stage")
                }
            }
        }

        val pushDockerImage by registering {
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
                            val platformArgs = if (platforms.isNotEmpty()) {
                                arrayOf("--platform", platforms.first())
                            } else {
                                emptyArray()
                            }
                            commandLine("docker", "pull", *platformArgs, tag)
                            errorOutput = eo

                        }
                        val shouldPushDev = project.gradle.taskGraph.hasTask(":pushDockerDevImage")
                        if (e.exitValue == 0 && !shouldPushDev) {
                            throw RuntimeException("tag already exists $tag")
                        }
                    }
                    doLast {
                        exec {
                            val cmd = buildXCmd(tag, push = true, stage = stage)
                            workingDir = dockerBuildDir
                            commandLine(*cmd)
                        }
                    }

                }
            }
        }

        val pushDockerDevImage by registering {
            dependsOn(pushDockerImage)
            group = DOCKER_GROUP
            description = "Pushes the docker image to the registry and tag it as dev"
            stages.forEach { stage ->
                val devTag = dockerTag("dev", stage = stage)
                doLast {
                    exec {
                        val cmd = buildXCmd(devTag, push = true, stage = stage)
                        workingDir = dockerBuildDir
                        commandLine(*cmd)
                    }
                }

            }
        }
    }
}
