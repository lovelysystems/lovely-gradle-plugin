plugins {
    base
    id("com.lovelysystems.gradle")
}

lovely {
    gitProject()
    pythonProject("python3")

    // build the default stage "" and the "base" stage defined in the Dockerfile
    dockerProject(
        "hub.example.com/lovely/exampleproject",
        stages = listOf("", "base"),
        platforms = listOf("linux/amd64", "linux/arm64", "linux/arm/v7")
    )

    with(dockerFiles) {
        from(tasks["writeVersion"].outputs)
    }
}


tasks["prepareDockerImage"].dependsOn("venv")

defaultTasks("buildDockerImage")
