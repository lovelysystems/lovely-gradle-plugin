package com.lovelysystems.gradle

import java.io.File
import java.text.SimpleDateFormat

val RELEASE_VERSION_PATTERN = Regex("([0-9]+)\\.([0-9]+)\\.([0-9]+)(-([0-9]+))?")
val RELEASE_LINE_PAT = Regex("^(20[0-9]{2}/[0-9]{2}/[0-9]{2})\\s($RELEASE_VERSION_PATTERN)$")
val HEADING_LINE_PAT = Regex("===+s*")
val CHANGELOG_DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")

/**
 * The version of a release which is in the form of <MAJOR>.<FEATURE>.<PATCH>[-<REVISION>]
 */
data class Version(val major: Int, val feature: Int, val patch: Int, val revision: Int = -1) : Comparable<Version> {

    companion object {
        /**
         * Creates a new version object from the given identifier String
         */
        fun fromIdent(ident: String): Version {
            val m =
                RELEASE_VERSION_PATTERN.matchEntire(ident)
                        ?: throw RuntimeException("Not a valid release version: $ident")
            return Version(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt(),
                if (m.groupValues[5].isNotEmpty()) m.groupValues[5].toInt() else -1
            )
        }
    }

    override fun compareTo(other: Version): Int {

        return major.compareTo(other.major).let {
            if (it != 0) it else feature.compareTo(other.feature).let {
                if (it != 0) it else patch.compareTo(other.patch).let {
                    if (it != 0) it else revision.compareTo(other.revision)
                }
            }
        }
    }

    override fun toString(): String {
        return """$major.$feature.$patch${if (revision == -1) "" else "-$revision"}"""
    }
}

class ChangeLog(private val file: File) {

    fun latestVersion(): Pair<String, Version>? {
        var seenHeadingLines = 0

        file.useLines {
            var previousLine = ""
            for (l in it) {
                if (HEADING_LINE_PAT.matches(l)) {
                    if (seenHeadingLines++ > 2) {
                        throw RuntimeException("Unable to find latest release Entry")
                    }
                    continue
                }
                if (seenHeadingLines == 3) {
                    val rel = previousLine.trim()
                    val m = RELEASE_LINE_PAT.matchEntire(rel)
                    if (m != null) {
                        CHANGELOG_DATE_FORMAT.parse(m.groupValues[1])
                        val version = Version.fromIdent(m.groupValues[2])
                        return Pair(m.groupValues[1], version)
                    } else if (rel == "unreleased") {
                        return null
                    }
                    throw RuntimeException("Release line not found in first heading")
                }
                previousLine = l
            }

        }
        throw RuntimeException("Release line not found in first heading")
    }
}