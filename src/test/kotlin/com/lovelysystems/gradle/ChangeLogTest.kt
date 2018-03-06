package com.lovelysystems.gradle

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEqual
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

class ChangeLogTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun testLatestEntryUnreleased() {
        val f = tmp.root.resolve("unreleasedLog.rst")
        f.writeText(unreleasedLog)
        val cl = ChangeLog(f)
        cl.latestVersion() shouldBe null
    }

    @Test
    fun testLatestEntryReleased() {
        val f = tmp.root.resolve("releasedLog.rst")
        f.writeText(releasedLog)
        val latestVersion = ChangeLog(f).latestVersion()!!
        latestVersion.second.toString() shouldBeEqualTo "0.0.2"
        latestVersion.first shouldBeEqualTo "2018/01/15"
    }

    @Test
    fun testVersionOrdering() {
        val expected = listOf("0.1.1", "0.1.2", "0.3.0", "0.10.1")
        expected.map { Version(it) }
            .sorted().map { it.toString() } shouldEqual expected
    }
}