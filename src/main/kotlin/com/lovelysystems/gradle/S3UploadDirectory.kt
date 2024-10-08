package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.internal.util.Mimetype
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import java.io.File
import java.util.concurrent.ExecutionException

/**
 * Uploads a directory to the configured S3 bucket.
 */
abstract class S3UploadDirectory : DefaultTask() {

    @get:Input
    abstract val bucket: Property<String>

    @get:Input
    abstract val sourceDirectory: Property<File>

    @get:Input
    @get:Optional
    @Deprecated("Use profileOverride instead")
    abstract val profile: Property<String?>

    @get:Input
    @get:Optional
    @Deprecated("Use regionOverride instead")
    abstract val region: Property<Region?>

    @get:Input
    @get:Optional
    abstract val profileOverride: Property<String?>

    @get:Input
    @get:Optional
    abstract val regionOverride: Property<String?>

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

        val profile = profileOverride.orNull ?: profile.orNull ?: project.awsSettings.profile
        val region = regionOverride.orNull ?: region.orNull?.toString() ?: project.awsSettings.region

        val s3Client = S3AsyncClient.builder()
            .credentialsProvider(
                AwsCredentialsProviderChain.of(
                    EnvironmentVariableCredentialsProvider.create(),
                    ProfileCredentialsProvider.create(profile),
                )
            )
            .region(Region.of(region))
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
            // Sets the correct content type for each file
            val uploadTransformer: (t: UploadFileRequest.Builder) -> Unit = { uploadBuilder ->
                val uploadFileRequest = uploadBuilder.build()
                val contentType = Mimetype.getInstance().getMimetype(uploadFileRequest.source())
                val putRequest = uploadFileRequest.putObjectRequest().toBuilder().contentType(contentType).build()
                uploadBuilder.putObjectRequest(putRequest)
            }
            val req = UploadDirectoryRequest
                .builder()
                .bucket(bucket.get())
                .s3Prefix(prefix.get())
                .source(sourceDirectory.get().toPath())
                .uploadFileRequestTransformer(uploadTransformer)
                .build()

            val result = transferManager.uploadDirectory(req).completionFuture().join()

            result.failedTransfers().takeIf { it.isNotEmpty() }?.let { failedTransfers ->
                throw RuntimeException("Failed to upload: $failedTransfers")
            }
        }
    }

}
