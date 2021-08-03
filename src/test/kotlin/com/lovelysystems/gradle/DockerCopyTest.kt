package com.lovelysystems.gradle

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DockerCopyTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    @Test
    fun test_simple_run() {

        folder.root.resolve("build.gradle.kts").writeText(
            """
                import com.lovelysystems.gradle.DockerCopy

                plugins {
                    id("com.lovelysystems.gradle")
                }
                
                val getIt by tasks.registering(DockerCopy::class) {
                    image.set("alpine:3.14")
                    srcPath.set("/etc/motd")
                    outputs.files(buildDir.resolve("motd"))
                }
            """
        )

        val result = GradleRunner.create().withPluginClasspath()
            .withProjectDir(folder.root)
            .withArguments("getIt")
            .build()
        result.task(":getIt")?.outcome shouldBe TaskOutcome.SUCCESS
        folder.root.resolve("build/motd").readText() shouldContain "Welcome to Alpine!"
    }
}