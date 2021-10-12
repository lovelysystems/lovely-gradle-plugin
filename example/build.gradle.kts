plugins {
    base
    id("com.lovelysystems.gradle")
}

lovely {
    gitProject()
    pythonProject("python3")

    // build the default stage "" and the "base" stage defined in the Dockerfile
    dockerProject("hub.example.com/lovely/exampleproject", stages = listOf("", "base"))

    with(dockerFiles) {
        from(tasks["writeVersion"].outputs)
    }
}


tasks["prepareDockerImage"].dependsOn("venv")

defaultTasks("buildDockerImage")
