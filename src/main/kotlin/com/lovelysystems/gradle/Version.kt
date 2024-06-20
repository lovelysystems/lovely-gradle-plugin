package com.lovelysystems.gradle

val RELEASE_VERSION_PATTERN = Regex("([0-9]+)\\.([0-9]+)\\.([0-9]+)(-([0-9]+))?")

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

        return major.compareTo(other.major).let { majorDiff ->
            if (majorDiff != 0) majorDiff else feature.compareTo(other.feature).let { featureDiff ->
                if (featureDiff != 0) featureDiff else patch.compareTo(other.patch).let { patchDiff ->
                    if (patchDiff != 0) patchDiff else revision.compareTo(other.revision)
                }
            }
        }
    }

    override fun toString(): String {
        return """$major.$feature.$patch${if (revision == -1) "" else "-$revision"}"""
    }
}
