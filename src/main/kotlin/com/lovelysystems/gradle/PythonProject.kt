package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File

open class PythonSettings(val project: Project, val pythonExecutable: String) {

    val envDir = project.file("v")
    val binDir = envDir.resolve("bin")
    val pip = binDir.resolve("pip")
    val python = binDir.resolve("python")
    val devReqFile = project.file("dev-requirements.txt")
    val reqFile = project.file("requirements.txt")
    val pipSync = binDir.resolve("pip-sync")
    val pipCompile = binDir.resolve("pip-compile")
    val versionFile = project.file("VERSION.txt")
    val setupFile = project.file("setup.py")
    val reqInFile = project.file("requirements.in")
    val devReqInFile = project.file("dev-requirements.in")

    val requirementsFile: File
        get() = if (setupFile.exists()) setupFile else reqInFile
}

open class VenvTask : DefaultTask() {

    init {
        group = "Bootstrap"
        description = "Bootstraps a python virtual environment"
        outputs.files(
            project.pythonSettings.pip,
            project.pythonSettings.python,
            project.pythonSettings.pipSync
        )
    }

    @TaskAction
    fun run() {
        project.exec {
            commandLine(project.pythonSettings.pythonExecutable, "-m", "venv", "--clear", project.pythonSettings.envDir)
        }
        project.exec {
            commandLine(
                project.pythonSettings.pip, "install", "--upgrade",
                "pip==19.3.1",
                "setuptools==41.6.0",
                "pip-tools==4.2.0"
            )
        }
    }
}

open class DevTask : DefaultTask() {

    init {
        group = "Bootstrap"
        description = "Nails requirements and installs all project dependencies into the venv"
        dependsOn("venv", "writeVersion")
        inputs.files(
            project.pythonSettings.setupFile,
            project.pythonSettings.reqInFile,
            project.pythonSettings.devReqInFile
        )
        outputs.files(
            project.pythonSettings.reqFile,
            project.pythonSettings.devReqFile
        )
    }

    @TaskAction
    fun run() {
        var devReqFile = project.pythonSettings.reqFile
        val reqInFile = project.pythonSettings.requirementsFile

        // nail requirements
        if (reqInFile.exists())
            project.exec {
                commandLine(
                    project.pythonSettings.pipCompile,
                    reqInFile.toRelativeString(project.projectDir),
                    "--output-file", project.pythonSettings.reqFile.toRelativeString(project.projectDir)
                )
            }

        // nail dev requirements
        if (project.pythonSettings.devReqInFile.exists()) {
            devReqFile = project.pythonSettings.devReqFile
            project.exec {
                commandLine(
                    project.pythonSettings.pipCompile,
                    project.pythonSettings.devReqInFile.toRelativeString(project.projectDir),
                    "--output-file", project.pythonSettings.devReqFile.toRelativeString(project.projectDir)
                )
            }
        }

        // sync venv
        project.exec {
            commandLine(project.pythonSettings.pipSync, devReqFile)
        }

        if (project.pythonSettings.setupFile.exists())
            project.exec {
                commandLine(
                    project.pythonSettings.pip, "install", "--no-deps", "-e", project.projectDir
                )
            }
    }
}

open class PyTestTask : DefaultTask() {

    init {
        group = "Verification"
        description = "Runs PyTest tests"
        dependsOn("dev")
        onlyIf {
            it.project.pythonSettings.binDir.resolve("pytest").exists()
        }
    }

    @TaskAction
    fun run() {
        val testRoot = if (project.projectDir.resolve("tests/pytest.ini").exists()) {
            project.projectDir.resolve("tests")
        } else {
            project.projectDir
        }
        project.exec {
            commandLine(project.pythonSettings.binDir.resolve("pytest"), "-s", testRoot)
        }
    }
}

fun Project.pythonProject(pythonExecutable: String) {
    project.extensions.create<PythonSettings>("python", project, pythonExecutable)

    tasks.register("writeVersion") {
        val out = file("VERSION.txt")
        outputs.file(out)
        // since we have no inputs, check if we need to run
        outputs.upToDateWhen {
            out.takeIf { it.exists() }?.readText() == project.version.toString()
        }
        doLast {
            pythonSettings.versionFile.writeText(project.version.toString())
        }
    }

    tasks.register<VenvTask>("venv")
    tasks.register<DevTask>("dev")
    tasks.register<PyTestTask>("test")
    tasks.register("sdist") {
        dependsOn("venv", "writeVersion")
        inputs.files(fileTree("src"), pythonSettings.setupFile, "MANIFEST.in")
        val out = buildDir.resolve("sdist")
        outputs.dir(out)
        out.deleteRecursively()
        doLast {
            exec {
                commandLine(pythonSettings.python, pythonSettings.setupFile, "sdist", "--dist-dir", out)
            }
        }
    }
}

val Project.pythonSettings: PythonSettings get() = extensions["python"] as PythonSettings
