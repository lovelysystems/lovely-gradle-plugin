package com.lovelysystems.gradle

import org.junit.rules.TemporaryFolder
import java.io.File

fun LSGit.createVersionedFile(
        name: String, commit: Boolean = true, tag: String = "",
        content: String? = null
) {
    File("${dir.absolutePath}/$name").printWriter().use { out ->
        out.print(content ?: "content of $name")
    }
    gitCmd("add", name)
    if (commit) {
        gitCmd("commit", "-m", "'added $name'")
    }
    if (tag.isNotEmpty()) {
        gitCmd("tag", tag)
    }
}

fun createSampleRepos(tmp: TemporaryFolder): Pair<LSGit, LSGit> {
    val upstreamPath = tmp.newFolder("upstream")
    val downstreamPath = tmp.newFolder("downstream")

    val upstream = LSGit(upstreamPath)
    upstream.gitCmd("init", "--initial-branch=main")
    upstream.gitCmd("config", "receive.denyCurrentBranch", "warn")
    upstream.createVersionedFile("CHANGES.md", content = releasedLog["md"])
    upstream.gitCmd("tag", "-a", "0.0.1", "-m", "release tag from upstream")
    upstream.createVersionedFile(".gitignore", content = ".gradle\n")

    val downstream = LSGit(downstreamPath)
    downstream.gitCmd("clone", "${upstreamPath.absolutePath}/.git", "./")

    return Pair(upstream, downstream)
}