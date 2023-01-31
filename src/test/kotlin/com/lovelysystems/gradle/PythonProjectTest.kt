package com.lovelysystems.gradle

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class PythonProjectTest {

    @Test
    fun testPep440Version() {
        // no release yet
        pep440Version("757ea77") shouldBeEqualTo "0.0+757ea77"
        // dirty commit w/o release
        pep440Version("757ea77.dirty") shouldBeEqualTo "0.0+757ea77.dirty"
        // proper release
        pep440Version("1.2") shouldBeEqualTo "1.2"
        // proper release with dirty changes
        pep440Version("0.64.0.dirty") shouldBeEqualTo "0.64.0+dirty"
        // release with additional commits
        pep440Version("1.2-2-757ea77") shouldBeEqualTo "1.2+2.757ea77"
        // release with additional commits and dirty state
        pep440Version("1.2-2-757ea77.dirty") shouldBeEqualTo "1.2+2.757ea77.dirty"
        // unlimited release segments
        pep440Version("1.234.5.6-4-757ea77.dirty") shouldBeEqualTo "1.234.5.6+4.757ea77.dirty"
        pep440Version("123.4.55.6-4-757ea77.dirty") shouldBeEqualTo "123.4.55.6+4.757ea77.dirty"

        // if version can't be parsed - simply return it
        pep440Version("a0.63.1-8-g9e7b2ab.dirty") shouldBeEqualTo "a0.63.1-8-g9e7b2ab.dirty"
        // also proper version numbers (though not created by this plugin) are not converted
        pep440Version("1.2.3.rc40") shouldBeEqualTo "1.2.3.rc40"
    }

}