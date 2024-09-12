import com.lovelysystems.gradle.isProductionVersion

plugins {
    base
    id("com.lovelysystems.gradle")
}

val awsProfile = "lovely"
lovely {
    gitProject()
    pythonProject("python3")
    awsProject(awsProfile) {
        /**
         * The configured tasks don't actually works as the buckets/objects don't actually exists.
         * They are configured to demonstrate how to create these tasks
         */
        downloadTasks = listOf({
            taskName = "downloadFile"
            bucket = "some-bucket"
            targetFile = File(buildDir, "samples.zip")
            key = "samples.zip"
            region = "eu-west-1"
        }, {
            taskName = "downloadSecondFile"
            bucket = "some-bucket"
            targetFile = File(buildDir, "samples-second.zip")
            key = "samples.zip"
        })

        uploadTasks = listOf({
            taskName = "uploadTestContentToS3"
            bucket = "some-bucket"
            sourceDirectory = File(buildDir, "test")
            prefix = "some-prefix"
        })
    }

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

val releaseTestContent by tasks.registering {
    group = "Release"
    description = "Release some content to S3"
    val execute = true
    if (execute) dependsOn("uploadSomeContentToS3")
    onlyIf {
        execute
    }
}

tasks["prepareDockerImage"].dependsOn("venv")

defaultTasks("buildDockerImage")
