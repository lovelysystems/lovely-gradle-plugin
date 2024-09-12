package com.lovelysystems.gradle

import com.lovelysystems.gradle.aws.uploadS3Directory
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.regions.Region
import java.io.File

/**
 * Uploads a directory to the configured S3 bucket.
 */
@Deprecated("Configure a upload task directly with the awsProject plugin")
abstract class S3UploadDirectory : DefaultTask() {

    @get:Input
    abstract val profile: Property<String>

    @get:Input
    abstract val bucket: Property<String>

    @get:Input
    abstract val sourceDirectory: Property<File>

    @get:Input
    @get:Optional
    abstract val region: Property<Region>

    @get:Input
    @get:Optional
    abstract val prefix: Property<String>

    @get:Input
    @get:Optional
    abstract val overwrite: Property<Boolean>

    init {
        region.convention(Region.EU_CENTRAL_1)
        prefix.convention("")
        overwrite.convention(false)
    }

    @TaskAction
    fun upload() {
        uploadS3Directory(
            sourceDirectory.get(),
            profile.get(),
            region.get(),
            bucket.get(),
            prefix.get(),
            overwrite.get()
        )
    }
}

