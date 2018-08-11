package com.lovelysystems.gradle

import java.io.File
import java.text.SimpleDateFormat

val CHANGELOG_NAMES = arrayListOf("CHANGES.md", "CHANGES.rst", "CHANGES.txt")
val CHANGELOG_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

fun parseChangeLog(file: File): ChangeLog {
    val changeLogFile = if (file.isDirectory) {
        file.listFiles { f -> CHANGELOG_NAMES.contains(f.name) }.firstOrNull()
                ?: throw IllegalArgumentException("No changelog found, allowed names are $CHANGELOG_NAMES")
    } else {
        file
    }
    return when (changeLogFile.extension) {
        "md" -> MDChangeLog(changeLogFile)
        "rst", "txt" -> RSTChangeLog(changeLogFile)
        else -> {
            throw IllegalArgumentException("Unknown file extension for changelog '${file.extension}'")
        }
    }
}

abstract class ChangeLog(val file: File) {
    abstract fun latestVersion(): Pair<String, Version>?
}

class MDChangeLog(file: File) : ChangeLog(file) {

    companion object {
        val RELEASE_LINE_PAT = Regex("^##\\s(20[0-9]{2}-[0-9]{2}-[0-9]{2})\\s/\\s($RELEASE_VERSION_PATTERN)$")
    }

    override fun latestVersion(): Pair<String, Version>? {

        file.useLines {
            for (l in it) {
                if (l.startsWith("## ")) {
                    val rel = l.trim()
                    val m = RELEASE_LINE_PAT.matchEntire(rel)
                    if (m != null) {
                        val day = CHANGELOG_DATE_FORMAT.parse(m.groupValues[1])
                        val version = Version.fromIdent(m.groupValues[2])
                        return Pair(CHANGELOG_DATE_FORMAT.format(day), version)
                    } else if (rel == "## unreleased") {
                        return null
                    }
                    throw RuntimeException("Release line not found in first heading")
                }
            }
        }
        throw RuntimeException("No release line found")
    }
}

class RSTChangeLog(file: File) : ChangeLog(file) {

    companion object {
        val RELEASE_LINE_PAT = Regex("^(20[0-9]{2}/[0-9]{2}/[0-9]{2})\\s($RELEASE_VERSION_PATTERN)$")
        val HEADING_LINE_PAT = Regex("===+s*")
        val RST_CHANGELOG_DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd")
    }

    override fun latestVersion(): Pair<String, Version>? {
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
                        val day = RST_CHANGELOG_DATE_FORMAT.parse(m.groupValues[1])
                        val version = Version.fromIdent(m.groupValues[2])
                        return Pair(CHANGELOG_DATE_FORMAT.format(day), version)
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