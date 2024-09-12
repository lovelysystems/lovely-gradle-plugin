package com.lovelysystems.gradle.aws

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

fun uploadS3Directory(
    sourceDirectory: File,
    profile: String,
    region: Region,
    bucket: String,
    prefix: String,
    overwrite: Boolean
) {
    if (!sourceDirectory.isDirectory) {
        throw RuntimeException("source $sourceDirectory is not a directory")
    }

    val s3Client = S3AsyncClient.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(profile))
        .region(region)
        .build()

    HeadBucketRequest.builder().bucket(bucket).build().let { req ->
        try {
            s3Client.headBucket(req).get()
        } catch (e: ExecutionException) {
            when (e.cause) {
                is NoSuchBucketException -> throw RuntimeException("Bucket $bucket does not exist")
                else -> throw e
            }
        }
    }

    val listResponse = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build()
        .let { req -> s3Client.listObjectsV2(req) }

    if (!overwrite && listResponse.get().keyCount() > 0) {
        throw RuntimeException("destination '$prefix' already exists")
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
            .bucket(bucket)
            .s3Prefix(prefix)
            .source(sourceDirectory.toPath())
            .uploadFileRequestTransformer(uploadTransformer)
            .build()

        val result = transferManager.uploadDirectory(req).completionFuture().join()

        result.failedTransfers().takeIf { it.isNotEmpty() }?.let { failedTransfers ->
            throw RuntimeException("Failed to upload: $failedTransfers")
        }
    }
}
