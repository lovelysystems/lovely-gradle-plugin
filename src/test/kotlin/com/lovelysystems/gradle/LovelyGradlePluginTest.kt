package com.lovelysystems.gradle

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LovelyGradlePluginTest {

    @get:Rule
    val tmp = TemporaryFolder()

    val config = """
             plugins {
              id 'com.lovelysystems.gradle'
             }
             """

    @Test
    fun testGitGetsNotApplied() {
        tmp.root.resolve("build.gradle").writeText(config)
        val runner = GradleRunner.create().withProjectDir(tmp.root)
            .withPluginClasspath().withDebug(true)
        val r = runner.withArguments("tasks", "--stacktrace", "-q").build()
        r.output shouldNotContain "printVersion"
    }

    @Test
    fun testCreateTag() {
        val (upstream, downstream) = createSampleRepos(tmp)
        upstream.createVersionedFile(
            "build.gradle", content = config + """
             lovely {
                 gitProject()
             }
             """
        )
        downstream.pull().call()

        val runner = GradleRunner.create().withPluginClasspath().withProjectDir(downstream.repository.workTree)

        var r = runner.withArguments("createTag", "--stacktrace", "-q").build()
        r.output.trim() shouldBeEqualTo "Created and pushed tag 0.0.2 with release date 2018/01/15"

        r = runner.withArguments("printVersion", "-q", "--stacktrace", "-q").build()
        r.output.trim() shouldBeEqualTo "0.0.2"
    }
}
