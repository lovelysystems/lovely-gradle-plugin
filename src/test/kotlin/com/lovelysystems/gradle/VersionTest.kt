package com.lovelysystems.gradle

import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.junit.Test

class VersionTest {

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