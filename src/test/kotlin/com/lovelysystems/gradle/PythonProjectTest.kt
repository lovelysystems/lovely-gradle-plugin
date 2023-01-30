package com.lovelysystems.gradle

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class PythonProjectTest {

    @Test
    fun testPep440Version() {
        // no release yet
        pep440Version("gcbcdbe6") shouldBeEqualTo "0.0+gcbcdbe6"
        // dirty commit w/o release
        pep440Version("gcbcdbe6.dirty") shouldBeEqualTo "0.0+gcbcdbe6.dirty"
        // proper release
        pep440Version("1.2") shouldBeEqualTo "1.2"
        // release with additional commits
        pep440Version("1.2-2-gcbcdbe6") shouldBeEqualTo "1.2+2-gcbcdbe6"
        // release with additional commits and dirty state
        pep440Version("1.2-2-gcbcdbe6.dirty") shouldBeEqualTo "1.2+2-gcbcdbe6.dirty"
        // unlimited release segments
        pep440Version("1.2.3.4.5-4-gcbcdbe6.dirty") shouldBeEqualTo "1.2.3.4.5+4-gcbcdbe6.dirty"
        // proper pep440 version not converted
        pep440Version("1.2.3.dev0") shouldBeEqualTo "1.2.3.dev0"
        pep440Version("1.2.3.a1") shouldBeEqualTo "1.2.3.a1"
        pep440Version("1.2.3.b20") shouldBeEqualTo "1.2.3.b20"
        pep440Version("1.2.3.rc40") shouldBeEqualTo "1.2.3.rc40"
        pep440Version("1.2.3.post5") shouldBeEqualTo "1.2.3.post5"

        // if version can't be parsed - simply return it
        pep440Version("unsupported.version") shouldBeEqualTo "unsupported.version"
    }

}