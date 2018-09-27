package com.lovelysystems.gradle

import org.amshove.kluent.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LSGitTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun testDescribe() {

        val g = LSGit(tmp.root)
        g.gitCmd("init")

        g.describe() shouldBeEqualTo "unversioned"

        g.createVersionedFile("x.txt", tag = "0.0.0")

        g.describe() shouldBeEqualTo "0.0.0"

        g.createVersionedFile("y.txt", false)

        g.describe() shouldBeEqualTo "0.0.0.dirty"

        g.gitCmd("commit", "-m", "added x.txt")

        g.describe() shouldMatch "^0.0.0-1-[^.]+$"
    }

    @Test
    fun testCheckProductionTag() {

        val upstreamPath = tmp.newFolder("upstream")
        val downstreamPath = tmp.newFolder("downstream")

        val upstream = LSGit(upstreamPath)
        upstream.gitCmd("init")
        upstream.createVersionedFile("a.txt", tag = "0.0.1")

        val downstream = LSGit(downstreamPath)
        downstream.gitCmd("clone", "${upstreamPath.absolutePath}/.git", "./")

        downstream.createVersionedFile("b.txt", tag = "0.0.2")

        val g = LSGit(downstreamPath)

        g.describe() shouldBeEqualTo "0.0.2"

        // 0.0.1 is a valid production Tag becaus it points to the same id on origin
        g.validateProductionTag("0.0.1")

        var func = { g.validateProductionTag("0.0.2") }
        func shouldThrow RuntimeException::class withMessage "Local tag 0.0.2 does not exist on origin upstream"

        upstream.gitCmd("tag", "0.0.2")

        // the tag differs
        func = { g.validateProductionTag("0.0.2") }
        func shouldThrow RuntimeException::class
    }

    @Test
    fun testRemoteBranchValidation() {

        val downstream = createSampleRepos(tmp).second


        downstream.createVersionedFile("some.txt")

        downstream.gitCmd("checkout", "-b", "my-work-branch")
        downstream.gitCmd("push", "--set-upstream", "origin", "my-work-branch")
        downstream::validateHeadIsOnValidRemoteBranch shouldThrow RuntimeException::class withMessage
                "The current HEAD is not in sync with any valid remote branch, it points to [origin/my-work-branch]"


        downstream.gitCmd("push", "--set-upstream", "origin", "master")
        downstream.validateHeadIsOnValidRemoteBranch()


        downstream.gitCmd("fetch")
        downstream.gitCmd("checkout", "-b", "release/0.2")
        downstream.createVersionedFile("other.txt")
        downstream.gitCmd("push", "--set-upstream", "origin", "release/0.2")
        downstream.validateHeadIsOnValidRemoteBranch()
    }

    @Test
    fun testDefaultBranch() {
        val upstream = createSampleRepos(tmp).first
        upstream.gitCmd("checkout", "-b", "develop")
        upstream.createVersionedFile("new.txt")

        val newClone = tmp.newFolder("newclone")
        val local2 = LSGit(newClone)
        local2.gitCmd("clone", upstream.dir.absolutePath, "./")

        local2.gitCmd("checkout", "develop")
        local2.validateHeadIsOnValidRemoteBranch()
    }

    @Test
    fun testCreateTag() {

        val downstream = createSampleRepos(tmp).second

        downstream.createVersionTag().second.toString() shouldBeEqualTo "0.0.2"
        downstream.validateProductionTag(downstream.describe())

        var func = { downstream.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Current head is already tagged with production tag 0.0.2"

        downstream.createVersionedFile("b.txt")
        func = { downstream.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Current HEAD does not point to any remote branches"

        downstream.gitCmd("push", "--set-upstream", "origin", "master")

        func = { downstream.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Version number superseded: 0.0.2 >= 0.0.2"

        downstream.gitCmd("checkout", "-b", "release/0.0.0")
        downstream.gitCmd("checkout", "master")

        downstream.createVersionedFile("newRealease.txt", tag = "0.1.0")

        downstream.gitCmd("checkout", "release/0.0.0")
        downstream.createVersionedFile("CHANGES.md", content =
        """
# Changes for some cool project

## 2018-01-16 / 0.0.3

- the hotfix in 0.0.3

## 2018-01-15 / 0.0.2

- the change in 0.0.2

## 2017-01-15 0.0.1

- initial version
"""
        )
        downstream.gitCmd("push", "--set-upstream", "origin", "release/0.0.0")
        downstream.createVersionTag().second.toString() shouldBeEqualTo "0.0.3"
        downstream.validateProductionTag(downstream.describe())
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

    @Test
    fun testLatestLocalGitTagVersion() {
        val g = LSGit(tmp.root)
        g.gitCmd("init")
        g.latestLocalGitTagVersion() shouldBe null
        tmp.root.resolve("some.txt").writeText("content")
        g.gitCmd("add", ".")
        g.gitCmd("commit", "-m", "some commit")
        g.gitCmd("tag", "not_a_valid_release_tag")
        g.latestLocalGitTagVersion() shouldBe null

        g.gitCmd("tag", "0.0.1")
        g.latestLocalGitTagVersion() shouldEqual Version(0, 0, 1)
        g.gitCmd("checkout", "-b", "release")
        g.createVersionedFile("release.txt", tag = "0.1.0")
        g.latestLocalGitTagVersion() shouldEqual Version(0, 1, 0)
        g.gitCmd("checkout", "master")
        g.latestLocalGitTagVersion() shouldEqual Version(0, 0, 1)
    }
}