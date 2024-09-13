import com.lovelysystems.gradle.S3DownloadFile
import com.lovelysystems.gradle.S3UploadDirectory

plugins {
    base
    id("com.lovelysystems.gradle")
}

val awsProfile = "lovely"
lovely {
    gitProject()
    pythonProject("python3")
    awsProject(awsProfile, "eu-central-1")

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

/**
 * The S3 tasks don't actually work as the buckets/objects don't exist.
 * They are there to demonstrate how to use them.
 */
val downloadFile by tasks.registering(S3DownloadFile::class) {
    bucket.set("some-bucket")
    targetFile.set(File(buildDir, "samples.zip"))
    key.set("samples.zip")
}

val uploadContent by tasks.registering(S3UploadDirectory::class) {
    bucket = "some-bucket"
    sourceDirectory = File(buildDir, "some-content")
    prefix = "some-target-prefix"
}

tasks["prepareDockerImage"].dependsOn("venv")

defaultTasks("buildDockerImage")
