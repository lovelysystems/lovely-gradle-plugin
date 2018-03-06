package com.lovelysystems.gradle

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldMatch
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
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
    fun testCreateTag() {

        val downstream = createSampleRepos(tmp).second

        val g = LSGit(downstream.repository.workTree)
        g.createVersionTag().second.toString() shouldBeEqualTo "0.0.2"
        g.validateProductionTag(g.describe())

        var func = { g.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Current head is already tagged with production tag 0.0.2"

        downstream.createVersionedFile("b.txt")
        func = { g.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Current head is not in sync with origin/master"

        downstream.push().call()

        func = { g.createVersionTag() }
        func shouldThrow RuntimeException::class withMessage "Version number superseded: 0.0.2 >= 0.0.2"
    }
}