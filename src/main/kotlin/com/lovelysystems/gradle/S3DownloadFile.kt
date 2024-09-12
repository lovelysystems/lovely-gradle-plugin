package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import kotlin.io.path.createFile

abstract class S3DownloadFile : DefaultTask() {

    @get:Input
    abstract val profile: Property<String>

    @get:Input
    abstract val bucket: Property<String>

    @get:OutputFile
    abstract val targetFile: Property<File>

    @get:Input
    @get:Optional
    abstract val region: Property<String>

    @get:Input
    @get:Optional
    abstract val key: Property<String>

    init {
        region.convention(Region.EU_CENTRAL_1.toString())
        outputs.upToDateWhen {
            targetFile.get().exists()
        }
    }

    @TaskAction
    fun download() {
        if (targetFile.get().exists()) {
            //even when adding a custom upToDateWhen the task might still be rerun when another upToDateWhen condition is false
            // e.g. when an input changed. This task should however ONLY download from S3 when the target doesn't already exist.
            // Because the other built-in upToDateWhen conditions cannot be ignored the implementation needs to abort if the target already exists
            logger.info("Target file {} exists. Skipping download from S3.", targetFile.get())
            return
        }

        val s3Client = S3AsyncClient.builder()
            .credentialsProvider(ProfileCredentialsProvider.create(profile.get()))
            .region(Region.of(region.get()))
            .build()

        HeadBucketRequest.builder().bucket(bucket.get()).build().let { req ->
            try {
                s3Client.headBucket(req).get()
            } catch (e: ExecutionException) {
                when (e.cause) {
                    is NoSuchBucketException -> throw RuntimeException("Bucket ${bucket.get()} does not exist")
                    else -> throw e
                }
            }
        }

        val listResponse = ListObjectsV2Request.builder().bucket(bucket.get()).prefix(key.get()).build()
            .let { req -> s3Client.listObjectsV2(req).get() }

        if (!listResponse.hasContents()) {
            throw RuntimeException("No object ${bucket.get()}/${key.get()} found")
        }

        if (listResponse.keyCount() > 1) {
            throw RuntimeException(
                "More than one object matching ${bucket.get()}/${key.get()} found. Found ${
                    listResponse.contents().map { it.key() }
                }"
            )
        }

        S3TransferManager.builder().s3Client(s3Client).build().use { transferManager ->
            val obj = listResponse.contents().first()
            val destination = Paths.get(targetFile.get().absolutePath)
            Files.createDirectories(destination.parent)
            destination.createFile()

            val downloadFile = transferManager.downloadFile(DownloadFileRequest.builder()
                .getObjectRequest { builder -> builder.bucket(bucket.get()).key(obj.key()) }
                .destination(destination)
                .build()
            )
            downloadFile.completionFuture().join()
        }
    }
}
