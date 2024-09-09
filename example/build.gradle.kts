plugins {
    base
    id("com.lovelysystems.gradle")
}

val awsProfile = "lovely"
lovely {
    gitProject()
    pythonProject("python3")
    awsProject(awsProfile)

    // build the default stage "" and the "base" stage defined in the Dockerfile
    dockerProject(
        "hub.example.com/lovely/exampleproject",
        stages = listOf("", "base"),
        platforms = listOf("linux/amd64", "linux/arm64", "linux/arm/v7"),
        buildPlatforms = listOf("linux/arm64")
    ) {
        from(tasks["writeVersion"].outputs)
    }
}


tasks["prepareDockerImage"].dependsOn("venv")

defaultTasks("buildDockerImage")
