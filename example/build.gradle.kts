plugins {
    base
    id("com.lovelysystems.gradle") version ("1.2.0")
}

lovely {
    gitProject()
    pythonProject("python3")
    dockerProject("hub.example.com/lovely/exampleproject")

    with(dockerFiles) {
        from(tasks["writeVersion"].outputs)
    }
}


tasks["prepareDockerImage"].dependsOn("venv")

defaultTasks("buildDockerImage")
