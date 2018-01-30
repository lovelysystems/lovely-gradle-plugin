package com.lovelysystems.gradle

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
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
        cl.latestVersion().second shouldBeEqualTo "unreleased"
        cl.latestVersion().first shouldBe ""
    }

    @Test
    fun testLatestEntryReleased() {
        val f = tmp.root.resolve("releasedLog.rst")
        f.writeText(releasedLog)
        val cl = ChangeLog(f)
        cl.latestVersion().second shouldBeEqualTo "0.0.2"
        cl.latestVersion().first shouldBeEqualTo "2018/01/15"
    }
}