package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists

abstract class S3DownloadFile : DefaultTask() {

    @get:Input
    abstract val bucket: Property<String>

    @get:OutputFile
    abstract val targetFile: Property<File>

    @get:Input
    @get:Optional
    abstract val profileOverride: Property<String?>

    @get:Input
    @get:Optional
    abstract val regionOverride: Property<String?>

    @get:Input
    @get:Optional
    abstract val key: Property<String>

    init {
        outputs.upToDateWhen {
            targetFile.get().exists()
        }
    }

    @TaskAction
    fun download() {
        val profile = profileOverride.orNull ?: project.awsSettings.profile
        val region = regionOverride.orNull ?: project.awsSettings.region

        val s3Client = S3AsyncClient.builder()
            .credentialsProvider(
                AwsCredentialsProviderChain.of(
                    EnvironmentVariableCredentialsProvider.create(),
                    ProfileCredentialsProvider.create(profile),
                )
            )
            .region(Region.of(region))
            .build()

        S3TransferManager.builder().s3Client(s3Client).build().use { transferManager ->
            val destination = Paths.get(targetFile.get().absolutePath)
            Files.createDirectories(destination.parent)
            //the target file can exists when the task is not up to date because e.g. a input (other than targetFile) changed.
            //So it is deleted as otherwise the download fails
            destination.deleteIfExists()
            destination.createFile()

            val downloadFile = transferManager.downloadFile(DownloadFileRequest.builder()
                .getObjectRequest { builder -> builder.bucket(bucket.get()).key(key.get()) }
                .destination(destination)
                .build()
            )
            try {
                downloadFile.completionFuture().join()
            } catch (e: Exception) {
                when (e.cause) {
                    is NoSuchBucketException -> throw RuntimeException("Bucket $bucket does not exist")
                    is NoSuchKeyException -> throw RuntimeException("No object $bucket/$key Found")
                    else -> throw e
                }
            }
        }
    }
}
