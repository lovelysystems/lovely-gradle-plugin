package com.lovelysystems.gradle

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.TagOpt
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

val PRODUCTION_VERSION_PATTERN = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+$")

fun isProductionVersion(version: String): Boolean {
    return PRODUCTION_VERSION_PATTERN.matchEntire(version) != null
}

class LSGit(private val dir: File) {

    private val git by lazy {
        try {
            Git.open(dir)
        } catch (e: RepositoryNotFoundException){
            throw RuntimeException("$dir is not a Git project")
        }

    }

    fun describe(): String {

        // use the git cli since jGit does not have that describe feature as we need it
        val proc = Runtime.getRuntime().exec(
            arrayOf("git", "describe", "--always", "--tags", "--dirty=.dirty"),
            emptyArray(),
            dir
        )

        proc.waitFor(2, TimeUnit.SECONDS)
        var res = "unversioned"
        if (proc.exitValue() == 0) {
            Scanner(proc.inputStream).use {
                res = it.nextLine()
            }
        } else {
            Scanner(proc.errorStream).use {
                while (it.hasNextLine()) System.err.println(it.nextLine())
            }
        }
        return res
    }

    private fun tagName(refName: String): String {
        return refName.substring(10)
    }

    private fun localTagRef(name: String): Ref {
        return git.tagList().call()!!.find { tagName(it.name) == name }
                ?: throw RuntimeException("Tag $name not found in local repository")
    }

    private fun remoteTags(): Map<String, Ref> {
        return git.lsRemote().setRemote("origin").setTags(true).setHeads(false).callAsMap()
    }

    fun validateProductionTag(name: String) {
        assert(isProductionVersion(name)) { "$name is not a production version tag" }

        val localTag = localTagRef(name)
        git.fetch().setTagOpt(TagOpt.FETCH_TAGS).call()
        val remoteTag = remoteTags()[localTag.name]
                ?: throw RuntimeException("Local tag $name does not exist on origin upstream")

        if (remoteTag.objectId != localTag.objectId) {
            throw RuntimeException(
                "The id of tag $name differs: local=${localTag.objectId} origin=${remoteTag.objectId}"
            )
        }
    }

    private fun validateCleanWorkTree() {
        val st = git.status().call()
        if (!st.isClean) {
            throw RuntimeException("Work directory is not clean")
        }
    }

    private fun validateHeadPushed() {
        val branch = git.repository.branch
        if (branch != "master") {
            throw RuntimeException("Current branch $branch is not master")
        }

        val st = BranchTrackingStatus.of(git.repository, "master")
                ?: throw RuntimeException("Failed to get tracking status of master")

        if (st.aheadCount > 0 || st.behindCount > 0) {
            throw RuntimeException("Branch master is not in sync with remote")
        }
    }

    fun createVersionTag(): String {
        validateCleanWorkTree()
        val changeLog = ChangeLog(git.repository.workTree.resolve("CHANGES.rst"))
        val (releaseDate, version) = changeLog.latestVersion()

        if (!isProductionVersion(version)) {
            throw RuntimeException("$version is not a valid production version identifier")
        }

        git.fetch().setTagOpt(TagOpt.FETCH_TAGS).call()
        validateHeadPushed()

        val currentVersion = describe()
        if (isProductionVersion(currentVersion)) {
            throw RuntimeException("Current head is already tagged with production tag $currentVersion")
        }

        val latestVersionLocal = git.tagList().call().map { tagName(it.name) }.filter { isProductionVersion(it) }.max()

        if (latestVersionLocal != null && latestVersionLocal >= version) {
            throw RuntimeException("Version number superseded: $latestVersionLocal >= $version")
        }

        git.tag().setName(version).setMessage("Release $version from $releaseDate").call()
        git.push().setPushTags().call()
        return version
    }
}