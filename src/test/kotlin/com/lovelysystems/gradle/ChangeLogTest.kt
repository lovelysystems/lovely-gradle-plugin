package com.lovelysystems.gradle

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

val unreleasedLog = """
=============================
Changes for some cool project
=============================

unreleased
==========

 - use generic database schema

2018/01/15 0.0.1
================

 - initial version

""".trim()

val releasedLog = """
=============================
Changes for some cool project
=============================

2018/01/15 0.0.2
================

 - the change in 0.0.2

2017/01/15 0.0.1
================

 - initial version

""".trim()

val releasedLogWithRevision = """
=============================
Changes for some cool project
=============================

2018/01/15 0.0.2-10
===================

 - the revision

""".trim()

class ChangeLogTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun createChangeLog(contents: String): ChangeLog {
        val f = tmp.root.resolve("CHANGES.rst")
        f.writeText(contents)
        return ChangeLog(f)
    }

    @Test
    fun testLatestEntryUnreleased() {
        createChangeLog(unreleasedLog).latestVersion() shouldBe null
    }

    @Test
    fun testLatestEntryReleased() {
        val latestVersion = createChangeLog(releasedLog).latestVersion()!!
        latestVersion.second.toString() shouldBeEqualTo "0.0.2"
        latestVersion.first shouldBeEqualTo "2018/01/15"
    }

    @Test
    fun testLatestEntryReleasedWithRevision() {
        val latestVersion = createChangeLog(releasedLogWithRevision).latestVersion()!!
        latestVersion.second.toString() shouldBeEqualTo "0.0.2-10"
        latestVersion.first shouldBeEqualTo "2018/01/15"
    }

    @Test
    fun testVersionOrdering() {
        val expected = listOf("0.1.1", "0.1.2", "0.3.0", "0.3.0-0", "0.3.0-2", "0.3.0-10", "0.10.1")
        expected.map { Version.fromIdent(it) }
            .sorted().map { it.toString() } shouldEqual expected
    }

    @Test
    fun testVersionFromIdent() {
        { Version.fromIdent("1.7.5-1some_trailing_stuff") } shouldThrow RuntimeException::class

        var v = Version.fromIdent("1.7.5")
        v.revision shouldEqual -1
        v.patch shouldEqual 5
        v.feature shouldEqual 7
        v.major shouldEqual 1

        v = Version.fromIdent("1.7.5-158")
        v.revision shouldEqual 158
        v.patch shouldEqual 5
        v.feature shouldEqual 7
        v.major shouldEqual 1
    }

    @Test
    fun testVersionEquals() {
        Version(1, 2, 3, 1) shouldEqual Version(1, 2, 3, 1)
    }
}