package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest
import java.io.File
import java.util.concurrent.ExecutionException

/**
 * Uploads a directory to the configured S3 bucket.
 */
abstract class S3UploadDirectory : DefaultTask() {

    @get:Input
    abstract val profile: Property<String>

    @get:Input
    abstract val bucket: Property<String>

    @get:Input
    abstract val sourceDirectory: Property<File>

    @get:Input
    @get:Optional
    abstract val prefix: Property<String>

    @get:Input
    @get:Optional
    abstract val overwrite: Property<Boolean>

    init {
        prefix.convention("")
        overwrite.convention(false)
    }

    @TaskAction
    fun upload() {
        if (!sourceDirectory.get().isDirectory) {
            throw RuntimeException("source ${sourceDirectory.get()} is not a directory")
        }

        val s3Client = S3CrtAsyncClient.builder()
            .credentialsProvider(ProfileCredentialsProvider.create(profile.get()))
            .region(Region.EU_CENTRAL_1)
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

        val listResponse = ListObjectsV2Request.builder().bucket(bucket.get()).prefix(prefix.get()).build()
            .let { req -> s3Client.listObjectsV2(req) }

        if (!overwrite.get() && listResponse.get().keyCount() > 0) {
            throw RuntimeException("destination '${prefix.get()}' already exists")
        }

        S3TransferManager.builder().s3Client(s3Client).build().use { transferManager ->
            val req = UploadDirectoryRequest.builder().bucket(bucket.get()).s3Prefix(prefix.get())
                .source(sourceDirectory.get().toPath()).build()
            val result = transferManager.uploadDirectory(req).completionFuture().join()

            result.failedTransfers().takeIf { it.isNotEmpty() }?.let { failedTransfers ->
                throw RuntimeException("Failed to upload: $failedTransfers")
            }
        }
    }

}
