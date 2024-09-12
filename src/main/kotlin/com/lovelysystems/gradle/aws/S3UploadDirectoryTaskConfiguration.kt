package com.lovelysystems.gradle.aws

import software.amazon.awssdk.regions.Region
import java.io.File

class S3UploadDirectoryTaskConfiguration {
    var taskName: String = "uploadDirectory"
    var sourceDirectory: File? = null
    var region: String = Region.EU_CENTRAL_1.toString()
    var bucket: String? = null
    var prefix: String = ""
    var overwrite: Boolean = false
}
