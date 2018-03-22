plugins {
    base
    id("com.lovelysystems.gradle") version ("0.0.6")
}

lovely {
    gitProject()
    dockerProject("hub.example.com/lovely/exampleproject")
}

val createVersionFile by tasks.creating {
    val f = file("VERSION.txt")
    outputs.files(f)
    f.writeText(version.toString())
}

lovely.dockerFiles.from(createVersionFile.outputs)
tasks["prepareDockerImage"].dependsOn(createVersionFile)

defaultTasks("buildDockerImage")