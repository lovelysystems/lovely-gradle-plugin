package com.lovelysystems.gradle

import java.io.File
import java.text.SimpleDateFormat

val RELEASE_LINE_PAT = Regex("^(20[0-9]{2}/[0-9]{2}/[0-9]{2})\\s([0-9]+\\.[0-9]+\\.[0-9]+)$")
val HEADING_LINE_PAT = Regex("===+s*")
val CHANGELOG_DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")
val RELEASE_VERSION_PATTERN = Regex("^([0-9]+)\\.([0-9]+)\\.([0-9]+)$")

class Version(ident: String) : Comparable<Version> {

    private var parts: IntArray

    init {
        val m = RELEASE_VERSION_PATTERN.matchEntire(ident) ?: throw RuntimeException("Not a valid release version: $ident")
        parts = m.groupValues.subList(1, 4).map { it.toInt() }.toIntArray()
    }

    override fun compareTo(other: Version): Int {
        var res = 0
        for (i in 0..2) {
            res = parts[i].compareTo(other.parts[i])
            if (res != 0) {
                break
            }
        }
        return res
    }

    override fun toString(): String {
        return parts.joinToString(".")
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
                        val version = Version(m.groupValues[2])
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