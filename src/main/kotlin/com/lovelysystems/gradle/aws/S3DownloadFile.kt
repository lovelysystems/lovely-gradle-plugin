package com.lovelysystems.gradle.aws

import org.gradle.api.Project
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

fun Project.downloadS3File(targetFile: File, bucket: String, key: String, region: String, profile: String) {
    if (targetFile.exists()) {
        //even when adding a custom upToDateWhen the task might still be rerun when another upToDateWhen condition is false
        // e.g. when an input changed. This task should however ONLY download from S3 when the target doesn't already exist.
        // Because the other built-in upToDateWhen conditions cannot be ignored the implementation needs to abort if the target already exists
        logger.info("Target file {} exists. Skipping download from S3.", targetFile)
        return
    }

    val s3Client = S3AsyncClient.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(profile))
        .region(Region.of(region))
        .build()

    S3TransferManager.builder().s3Client(s3Client).build().use { transferManager ->
        val destination = Paths.get(targetFile.absolutePath)
        Files.createDirectories(destination.parent)
        destination.createFile()

        try {
            val downloadFile =transferManager.downloadFile(DownloadFileRequest.builder()
                .getObjectRequest { builder -> builder.bucket(bucket).key(key) }
                .destination(destination)
                .build()
            )
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