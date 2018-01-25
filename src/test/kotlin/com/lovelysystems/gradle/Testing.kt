package com.lovelysystems.gradle

import org.eclipse.jgit.api.Git
import org.junit.rules.TemporaryFolder

fun Git.createVersionedFile(
    name: String, commit: Boolean = true, tag: String = "",
    content: String? = null
) {
    repository.workTree.resolve(name).writeText(content ?: "content of $name")
    add().addFilepattern(name).call()
    if (commit) {
        commit().setMessage("added $name").call()
    }
    if (tag.isNotEmpty()) {
        tag().setName(tag).call()
    }
}

fun createSampleRepos(tmp: TemporaryFolder): Pair<Git, Git> {
    val upstreamPath = tmp.newFolder("upstream")
    val downstreamPath = tmp.newFolder("downstream")

    val upstream = Git.init().setDirectory(upstreamPath).call()
    upstream.createVersionedFile("CHANGES.rst", content= releasedLog)
    upstream.tag().setName("0.0.1").setMessage("release tag from upstream").call()
    upstream.createVersionedFile(".gitignore", content = ".gradle\n")


    val downstream = Git.cloneRepository().setDirectory(downstreamPath).setURI(
        upstreamPath.toURI()
            .toASCIIString()
    ).call()
    return Pair(upstream, downstream)
}