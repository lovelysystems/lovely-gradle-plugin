package com.lovelysystems.gradle

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

private const val AWS_GROUP = "aws"

fun Project.awsProject(profile: String, ssoSessionSettings: SsoSessionSettings) {

    tasks {

        /**
         * Task to setup the AWS SSO session config if not already present. This is required to use AWS SSO and must be
         * setup only once.
         */
        val ssoSessionSetup by tasks.registering {
            group = AWS_GROUP
            description = "Setup lovely-sso session config"

            doLast {
                val userHome = System.getProperty("user.home")
                val configFile = Paths.get("$userHome/.aws/config")
                val config = Files.readAllLines(configFile)

                if (!config.contains("[sso-session ${SsoSessionSettings.NAME}]")) {
                    val sessionConfig = ssoSessionSettings.toAwsConfig()
                    Files.write(configFile, sessionConfig.toByteArray(), StandardOpenOption.APPEND)
                    println("Added ${SsoSessionSettings.NAME} session config to $configFile")
                }
            }
        }

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
            dependsOn(ssoSessionSetup)

            doLast {
                runCmd("[[ \$(aws configure --profile $profile list) && \$? -eq 0 ]]") { error ->
                    if (error.profileNotFoundError) {
                        val msg = """
                        AWS profile '$profile' is not configured:
                         - Run 'aws configure sso --profile ${profile}' to configure the profile.
                         - Use `${SsoSessionSettings.NAME}` when prompted for the 'SSO session name', for the rest use the default values.
                        """.trimIndent()
                        error(msg)
                    } else {
                        error("Failed to check if profile '$profile' is configured. Error: ${error.msg}")
                    }
                }

                runCmd("aws sts get-caller-identity --profile $profile") { error ->
                    if (error.ssoTokenError) runCmd("aws sso login --profile $profile")
                    else error("Failed to get caller identity for profile '$profile'. Error: ${error.msg}")
                }

                println("\nAWS SSO credentials for profile '$profile' are now configured and ready to be used.")
            }
        }

    }

}

data class SsoSessionSettings(
    val startUrl: String = "https://lovelysystems.awsapps.com/start",
    val region: String = "eu-central-1",
    val scopes: String = "sso:account:access",
) {
    companion object {
        const val NAME = "lovely-gradle-sso"
    }
}

private class AwsError(val msg: String) {
    val profileNotFoundError by lazy { msg.contains("could not be found") }
    val ssoTokenError by lazy { msg.contains("Error loading SSO Token") }
}

private fun SsoSessionSettings.toAwsConfig(): String =
    """
        [sso-session ${SsoSessionSettings.NAME}]
        sso_start_url = $startUrl
        sso_region = $region
        sso_registration_scopes = $scopes
    """.trimIndent()
