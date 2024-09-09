package com.lovelysystems.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
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
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists

abstract class S3DownloadFiles : DefaultTask() {

    @get:Input
    abstract val profile: Property<String>

    @get:Input
    abstract val bucket: Property<String>

    @get:Input
    abstract val targetDirectory: Property<File>

    @get:Input
    @get:Optional
    abstract val region: Property<Region>

    @get:Input
    @get:Optional
    abstract val prefix: Property<String>

    init {
        region.convention(Region.EU_CENTRAL_1)
        prefix.convention("")
    }

    @TaskAction
    fun download() {
        if (!targetDirectory.get().isDirectory) {
            throw RuntimeException("target ${targetDirectory.get()} is not a directory")
        }

        val s3Client = S3AsyncClient.builder()
            .credentialsProvider(ProfileCredentialsProvider.create(profile.get()))
            .region(region.get())
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
            .let { req -> s3Client.listObjectsV2(req).get() }

        if (!listResponse.hasContents()) {
            throw RuntimeException("No contents matching ${bucket.get()}/${prefix.get()} found")
        }

        S3TransferManager.builder().s3Client(s3Client).build().use { transferManager ->
            listResponse.contents().forEach { obj ->
                val destination = Paths.get(targetDirectory.get().absolutePath, obj.key())
                Files.createDirectories(destination.parent)
                destination.deleteIfExists()
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
}
