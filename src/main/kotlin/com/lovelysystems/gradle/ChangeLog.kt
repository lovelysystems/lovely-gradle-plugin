package com.lovelysystems.gradle

import java.io.File
import java.text.SimpleDateFormat

val RELEASE_LINE_PAT = Regex("^(20[0-9]{2}/[0-9]{2}/[0-9]{2})\\s([0-9]+\\.[0-9]+\\.[0-9]+)$")
val HEADING_LINE_PAT = Regex("===+s*")

val CHANGELOG_DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")

class ChangeLog(private val file: File) {

    fun latestVersion(): Pair<String, String> {
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
                        return Pair(m.groupValues[1], m.groupValues[2])
                    } else if (rel == "unreleased") {
                        return Pair("", rel)
                    }
                    throw RuntimeException("Release line not found in first heading")
                }
                previousLine = l
            }

        }
        throw RuntimeException("Release line not found in first heading")
    }
}