package com.lovelysystems.gradle.aws

import software.amazon.awssdk.regions.Region
import java.io.File

class S3DownloadTaskConfiguration {
    var taskName: String = "downloadFile"
    var targetFile: File? = null
    var region: String = Region.EU_CENTRAL_1.toString()
    var bucket: String? = null
    var key: String? = null
}
