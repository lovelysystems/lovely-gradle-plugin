package com.lovelysystems.gradle

import org.amshove.kluent.*
import org.eclipse.jgit.api.Git
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LSGitTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun testDescribe() {

        val g = LSGit(tmp.root)
        g.describe() shouldBeEqualTo "unversioned"

        val git = Git.init()
            .setDirectory(tmp.root)
            .call()

        g.describe() shouldBeEqualTo "unversioned"

        git.createVersionedFile("x.txt", tag = "0.0.0")

        g.describe() shouldBeEqualTo "0.0.0"

        git.createVersionedFile("y.txt", false)

        g.describe() shouldBeEqualTo "0.0.0.dirty"

        git.commit().setMessage("added x.txt").call()

        g.describe() shouldMatch "^0.0.0-1-[^.]+$"
    }

    @Test
    fun testCheckProductionTag() {

        val upstreamPath = tmp.newFolder("upstream")
        val downstreamPath = tmp.newFolder("downstream")

        val upstream = Git.init().setDirectory(upstreamPath).call()
        upstream.createVersionedFile("a.txt", tag = "0.0.1")

        val downstream = Git.cloneRepository().setDirectory(downstreamPath).setURI(
            upstreamPath.toURI()
                .toASCIIString()
        ).call()

        downstream.createVersionedFile("b.txt", tag = "0.0.2")

        val g = LSGit(downstreamPath)

        g.describe() shouldBeEqualTo "0.0.2"

        // 0.0.1 is a valid production Tag becaus it points to the same id on origin
        g.validateProductionTag("0.0.1")

        var func = { g.validateProductionTag("0.0.2") }
        func shouldThrow RuntimeException::class withMessage "Local tag 0.0.2 does not exist on origin upstream"

        upstream.tag().setName("0.0.2").call()

        // the tag differs
        func = { g.validateProductionTag("0.0.2") }
        func shouldThrow RuntimeException::class
    }

    @Test
    fun testRemoteBranchValidation() {

        val downstream = createSampleRepos(tmp).second

        val local = LSGit(downstream.repository.workTree)

        downstream.createVersionedFile("some.txt")

        local.gitCmd("checkout", "-b", "my-work-branch")
        local.gitCmd("push", "--set-upstream", "origin", "my-work-branch")
        local::validateHeadIsOnValidRemoteBranch shouldThrow RuntimeException::class withMessage
                "The current HEAD is not in sync with any valid remote branch, it points to [origin/my-work-branch]"


        local.gitCmd("push", "--set-upstream", "origin", "master")
        local.validateHeadIsOnValidRemoteBranch()


        local.gitCmd("fetch")
        local.gitCmd("checkout", "-b", "release/0.2")
        downstream.createVersionedFile("other.txt")
        local.gitCmd("push", "--set-upstream", "origin", "release/0.2")
        local.validateHeadIsOnValidRemoteBranch()
    }

    @Test
    fun testDefaultBranch() {
        val upstream = createSampleRepos(tmp).first
        val remote = LSGit(upstream.repository.workTree)
        remote.gitCmd("checkout", "-b", "develop")
        upstream.createVersionedFile("new.txt")

        val newClone = tmp.newFolder("newclone")
        Runtime.getRuntime().exec(
            arrayOf("git", "clone", upstream.repository.workTree.absolutePath, newClone.absolutePath)
        ).waitFor()
        val local2 = LSGit(newClone)
        local2.gitCmd("checkout", "develop")
        local2.validateHeadIsOnValidRemoteBranch()
    }

    @Test
    fun testCreateTag() {

        val downstream = createSampleRepos(tmp).second

        val g = LSGit(downstream.repository.workTree)
        g.createVersionTag().second.toString() shouldBeEqualTo "0.0.2"
        g.validateProductionTag(g.describe())

        var func = { g.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Current head is already tagged with production tag 0.0.2"

        downstream.createVersionedFile("b.txt")
        func = { g.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Current HEAD does not point to any remote branches"

        downstream.push().call()

        func = { g.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Version number superseded: 0.0.2 >= 0.0.2"
    }

    @Test
    fun testNoDir() {
        { LSGit(tmp.root.resolve("this file does not exist")) } shouldThrow AssertionError::class
    }

    @Test
    fun testErrorThrown() {
        val g = LSGit(tmp.root)
        ({ g.gitCmd("describe") }).shouldThrow(RuntimeException::class)
            .exceptionMessage shouldContain "git command failed: git describe\nfatal:"
    }

    @Test
    fun testErrorReturned() {
        val g = LSGit(tmp.root)
        g.gitCmd("describe") {
            "the error was -> ${it.reader().readText()}"
        } shouldStartWith "the error was -> fatal:"
    }
}