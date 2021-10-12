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

fun Project.dockerProject(repository: String, files: CopySpec, stages: List<String>) {

    val dockerBuildDir = project.buildDir.resolve("docker")

    fun Project.dockerTag(versionTag: String = version.toString(), stage: String = ""): String {
        if (stage.isEmpty()) {
            return "$repository:${versionTag}"
        } else {
            return "$repository:${versionTag}-$stage"
        }
    }

    tasks {

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

        val buildDockerImage by creating {
            dependsOn(prepareDockerImage)
            group = DOCKER_GROUP
            description = "Builds a docker image and tags it with version and dev"
            inputs.files(prepareDockerImage.outputs)

            doLast {
                stages.forEach { stage ->
                    logger.info("building docker image stage=$stage")
                    exec {
                        workingDir = dockerBuildDir
                        commandLine(
                            "docker", "build", ".",
                            "-t", project.dockerTag(stage = stage),
                            "-t", project.dockerTag("dev", stage = stage)
                        )
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
                            commandLine("docker", "push", tag)
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
                        commandLine("docker", "push", devTag)
                    }
                }

            }
        }
    }
}