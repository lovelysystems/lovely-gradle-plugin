package com.lovelysystems.gradle

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

val unreleasedLog = mapOf(
        "rst" to """
=============================
Changes for some cool project
=============================

unreleased
==========

 - use generic database schema

2018/01/15 0.0.1
================

 - initial version

""",
        "md" to """
# Changes for some cool project

## unreleased

- use generic database schema

## 2018-01-15 / 0.0.1

- initial version

"""
)

val releasedLog = mapOf(
        "rst" to """
=============================
Changes for some cool project
=============================

2018/01/15 0.0.2
================

 - the change in 0.0.2

2017/01/15 0.0.1
================

 - initial version

""",
        "md" to """
# Changes for some cool project

## 2018-01-15 / 0.0.2

- the change in 0.0.2

## 2017-01-15 0.0.1

- initial version
"""
)

val releasedLogWithRevision = mapOf(
        "rst" to """
=============================
Changes for some cool project
=============================

2018/01/15 0.0.2-10
===================

 - the revision

""",
        "md" to """
# Changes for some cool project

## 2018-01-15 / 0.0.2-10

- the revision
"""
)

val FILENAMES = listOf("CHANGES", "CHANGELOG")
val EXTENSIONS = listOf("rst", "md", "txt")

@RunWith(Parameterized::class)
class ChangeLogTest(private val filename: String) {

    @get:Rule
    val tmp = TemporaryFolder()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = FILENAMES.map { name -> EXTENSIONS.map { extension -> "$name.$extension" } }.flatten()

    }

    private fun createChangeLog(contents: Map<String, String>): ChangeLog {
        val f = createChangeLogFile(contents)
        return parseChangeLog(f)
    }

    fun createChangeLogFile(contentMap: Map<String, String>): File {
        val f = tmp.root.resolve(filename)
        println(f.name)
        val suffix = filename.split(".").last()
        println(suffix)
        val content = when (suffix) {
            "txt" -> contentMap["rst"]!!
            else -> contentMap[suffix]!!
        }
        f.writeText(content)
        return f
    }

    @Test
    fun testFindChangeLogFileViaDirectory() {
        val f = createChangeLogFile(unreleasedLog)
        parseChangeLog(f.parentFile).file shouldEqual f
    }

    @Test
    fun testLatestEntryUnreleased() {
        createChangeLog(unreleasedLog).latestVersion() shouldBe null
    }

    @Test
    fun testLatestEntryReleased() {
        val latestVersion = createChangeLog(releasedLog).latestVersion()!!
        latestVersion.second.toString() shouldBeEqualTo "0.0.2"
        latestVersion.first shouldBeEqualTo "2018-01-15"
    }

    @Test
    fun testLatestEntryReleasedWithRevision() {
        val latestVersion = createChangeLog(releasedLogWithRevision).latestVersion()!!
        latestVersion.second.toString() shouldBeEqualTo "0.0.2-10"
        latestVersion.first shouldBeEqualTo "2018-01-15"
    }
}