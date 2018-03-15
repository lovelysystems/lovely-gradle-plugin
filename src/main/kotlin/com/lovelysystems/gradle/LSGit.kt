package com.lovelysystems.gradle

import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

fun isProductionVersion(version: String): Boolean {
    return RELEASE_VERSION_PATTERN.matchEntire(version) != null
}

class LSGit(private val dir: File) {

    init {
        assert(dir.isDirectory) { "'$dir' is not a directory" }
    }

    fun gitCmd(vararg args: String, onError: ((InputStream) -> String)? = null): String {
        val cmd = arrayOf("git") + args
        val proc = Runtime.getRuntime().exec(
            cmd,
            emptyArray(),
            dir
        )
        proc.waitFor(10, TimeUnit.SECONDS)
        return if (proc.exitValue() == 0) {
            proc.inputStream.reader().readText().trim()
        } else {
            onError?.invoke(proc.errorStream) ?: throw RuntimeException(
                "git command failed: ${cmd.joinToString(" ")}\n${proc.errorStream.reader().readText()}"
            )
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

    private fun validateHeadIsMasterAndPushed() {
        val res = gitCmd("log", "--pretty=format:%D", "-n", "1")

        if (!res.startsWith("HEAD -> master")) {
            throw RuntimeException("Current head is not on master")
        }

        if (!res.contains("origin/master")) {
            throw RuntimeException("Current head is not in sync with origin/master")
        }
    }

    private fun latestLocalGitTagVersion(): Version? {
        val res = gitCmd("tag", "-l", "--sort=-v:refname").lines()
        return if (res.isEmpty()) null else Version.fromIdent(res.first { isProductionVersion(it) })
    }

    fun createVersionTag(): Pair<String, Version> {
        validateCleanWorkTree()
        val changeLog = ChangeLog(dir.resolve("CHANGES.rst"))
        val releaseInfo =
            changeLog.latestVersion() ?: throw RuntimeException("Changelog entry for release cannot be found")

        fetch()
        validateHeadIsMasterAndPushed()

        val currentVersion = describe()
        if (isProductionVersion(currentVersion)) {
            throw RuntimeException("Current head is already tagged with production tag $currentVersion")
        }

        val latestVersionLocal = latestLocalGitTagVersion()
        if (latestVersionLocal != null && latestVersionLocal >= releaseInfo.second) {
            throw RuntimeException("Version number superseded: $latestVersionLocal >= ${releaseInfo.second}")
        }

        gitCmd(
            "tag", "-a", "-m", "Release ${releaseInfo.second} from ${releaseInfo.first}",
            releaseInfo.second.toString()
        )
        gitCmd("push", "--tags", "-q")
        return releaseInfo
    }
}