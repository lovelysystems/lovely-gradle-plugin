package com.lovelysystems.gradle

import com.lovelysystems.gradle.aws.S3DownloadTaskConfiguration
import com.lovelysystems.gradle.aws.S3UploadDirectoryTaskConfiguration
import com.lovelysystems.gradle.aws.downloadS3File
import com.lovelysystems.gradle.aws.uploadS3Directory
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import software.amazon.awssdk.regions.Region
import java.io.ByteArrayOutputStream

private const val AWS_GROUP = "aws"

class AwsProjectConfiguration {
    var downloadTasks: List<S3DownloadTaskConfiguration.() -> Unit> = listOf()
    var uploadTasks: List<S3UploadDirectoryTaskConfiguration.() -> Unit> = listOf()
}

fun Project.awsProject(profile: String, configurationInit: AwsProjectConfiguration.() -> Unit) {
    val config = AwsProjectConfiguration().apply {
        configurationInit()
    }

    tasks {

        fun runCmd(cmd: String, onError: (AwsError) -> Unit = { error(it.msg) }) {
            ByteArrayOutputStream().use { errorOut ->
                val res = project.exec {
                    errorOutput = errorOut
                    isIgnoreExitValue = true
                    commandLine("sh", "-c", cmd)
                }
                if (res.exitValue != 0) {
                    val awsError = AwsError(errorOut.toString().trim())
                    onError(awsError)
                }
            }
        }

        config.downloadTasks.map { configInit -> S3DownloadTaskConfiguration().apply(configInit) }
            .forEach { taskConfiguration ->
                tasks.register(taskConfiguration.taskName) {
                    group = AWS_GROUP
                    description = "Download s3://${taskConfiguration.bucket}/${taskConfiguration.key}"

                    outputs.files(taskConfiguration.targetFile)
                    outputs.upToDateWhen {
                        taskConfiguration.targetFile?.exists() == true
                    }

                    doLast {
                        downloadS3File(
                            targetFile = requireNotNull(taskConfiguration.targetFile) { "Target file is required" },
                            bucket = requireNotNull(taskConfiguration.bucket) { "Bucket is required" },
                            key = requireNotNull(taskConfiguration.key) { "Key is required" },
                            profile = profile,
                            region = taskConfiguration.region
                        )
                    }
                }
            }
        config.uploadTasks.map { taskConfig -> S3UploadDirectoryTaskConfiguration().apply(taskConfig) }
            .forEach { taskConfig ->
                tasks.register(taskConfig.taskName) {
                    group = AWS_GROUP
                    description =
                        "Upload directory ${taskConfig.sourceDirectory} to s3://${taskConfig.bucket}/${taskConfig.prefix}"

                    doLast {
                        uploadS3Directory(
                            sourceDirectory = requireNotNull(taskConfig.sourceDirectory) { "Source directory is required" },
                            profile = profile,
                            region = Region.of(taskConfig.region),
                            bucket = requireNotNull(taskConfig.bucket) { "Bucket is required" },
                            prefix = taskConfig.prefix,
                            overwrite = taskConfig.overwrite,
                        )
                    }
                }
            }

        /**
         * Task to get AWS SSO credentials for the configured [profile].
         *
         * - If the profile is not configured, it will abort and ask to configure the profile first.
         * - If the profile is configured but the SSO token is expired, it will login to AWS SSO (automatically opens
         *   browser).
         */
        val ssoCredentials by registering {
            group = AWS_GROUP
            description = "Get credentials using AWS SSO"

            doLast {
                runCmd("aws sts get-caller-identity --profile $profile") { error ->
                    if (error.profileNotFoundError) {
                        val msg = """
                        AWS profile '$profile' is not configured. Run `aws configure sso --profile ${profile}` to
                        configure the profile.
                        """.trimIndent()
                        error(msg)
                    } else {
                        runCmd("aws sso login --profile $profile")
                    }
                }

                println("\nAWS SSO credentials for profile '$profile' are now configured and ready to be used.")
            }
        }

    }

}


private class AwsError(val msg: String) {
    val profileNotFoundError by lazy { msg.matches(Regex("The config profile \\(.+\\) could not be found")) }
}
