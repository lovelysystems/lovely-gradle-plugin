package com.lovelysystems.gradle

import java.io.File
import java.util.concurrent.TimeUnit

fun isProductionVersion(version: String): Boolean {
    return RELEASE_VERSION_PATTERN.matchEntire(version) != null
}

class LSGit(val dir: File) {

    init {
        assert(dir.isDirectory) { "'$dir' is not a directory" }
    }

    fun gitCmd(vararg args: String, onError: ((String) -> String)? = null): String {
        val cmd = listOf("git") + args

        val stdoutFile = createTempFile()
        val stderrFile = createTempFile()

        try {
            val proc = ProcessBuilder(cmd)
                .directory(dir)
                .redirectOutput(stdoutFile)
                .redirectError(stderrFile)
                .start()
            proc.waitFor(10, TimeUnit.SECONDS)
            val stdout = stdoutFile.readText().trim()
            val stderr = stderrFile.readText().trim()
            return if (proc.exitValue() == 0) {
                return stdout
            } else {
                onError?.invoke(stderr) ?: throw RuntimeException(
                    "git command failed: ${cmd.joinToString(" ")}\n$stderr"
                )
            }
        } finally {
            stderrFile.delete()
            stdoutFile.delete()
        }
    }

    fun describe() = gitCmd("describe", "--always", "--tags", "--dirty=.dirty") { "unversioned" }

    private fun fetch() {
        gitCmd("fetch", "--tags")
    }

    private fun localTagHash(name: String): String? {
        val res = gitCmd("show-ref", "--tags", "-s", name)
        return if (res.isBlank()) return null else res
    }

    private fun remoteTagHash(name: String): String? {
        val line = gitCmd("ls-remote", "--refs", "--tags", "-q", "origin", name)
        if (line.isBlank()) {
            return null
        }
        return line.split(Regex("\\s+")).first()
    }

    fun validateProductionTag(name: String) {
        assert(isProductionVersion(name)) { "$name is not a production version tag" }

        val localHash = localTagHash(name) ?: throw RuntimeException("Local tag $name not found")
        val remoteHash = remoteTagHash(name) ?: throw RuntimeException(
            "Local tag $name does not exist on origin upstream"
        )

        if (localHash != remoteHash) {
            throw RuntimeException(
                "The hash of the tag $name differs: local=$localHash origin=$remoteHash"
            )
        }
    }

    private fun validateCleanWorkTree() {
        if (gitCmd("status", "-s").isNotEmpty()) {
            throw RuntimeException("Work directory is not clean")
        }
    }

    fun validateHeadIsOnValidRemoteBranch() {

        val branches = gitCmd("branch", "-r", "--contains").lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (branches.isEmpty()) {
            throw RuntimeException("Current HEAD does not point to any remote branches")
        }

        if (branches.find {
                (it == "origin/master" || it.startsWith("origin/HEAD -> ") || it.startsWith("origin/release/"))
            } == null) {
            throw RuntimeException("The current HEAD is not in sync with any valid remote branch, it points to $branches")
        }
    }

    fun latestLocalGitTagPatchOfVersion(releaseVersion: Version? = null): Version? {
        var versions = gitCmd("tag", "-l", "--sort=-v:refname").lines().filter { isProductionVersion(it) }.map(Version.Companion::fromIdent)
        versions =
                if (releaseVersion != null)
                    versions.filter { it.major == releaseVersion.major && it.feature == releaseVersion.feature }
                else
                    versions
        return if (versions.isEmpty()) null
        else versions.reduce { latest, version -> if (version > latest) version else latest }
    }

    fun parseChangeLog(): ChangeLog {
        return parseChangeLog(dir)
    }

    fun createVersionTag(): Pair<String, Version> {
        validateCleanWorkTree()
        val changeLog = parseChangeLog()
        val releaseInfo =
            changeLog.latestVersion() ?: throw RuntimeException("Changelog entry for release cannot be found")

        fetch()
        validateHeadIsOnValidRemoteBranch()

        val currentVersion = describe()
        if (isProductionVersion(currentVersion)) {
            throw RuntimeException("Current head is already tagged with production tag $currentVersion")
        }

        val latestPatchOfVersionLocal = latestLocalGitTagPatchOfVersion(releaseInfo.second)
        if (latestPatchOfVersionLocal != null && latestPatchOfVersionLocal >= releaseInfo.second) {
            throw RuntimeException("Version number superseded: $latestPatchOfVersionLocal >= ${releaseInfo.second}")
        }

        gitCmd(
            "tag", "-a", "-m", "Release ${releaseInfo.second} from ${releaseInfo.first}",
            releaseInfo.second.toString()
        )
        gitCmd("push", "--tags", "--no-verify", "-q")
        return releaseInfo
    }
}