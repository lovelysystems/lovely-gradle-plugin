import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.0.0"
    id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
    mavenCentral()
}

group = "com.lovelysystems"
version = "1.15.1"

val pluginId = "com.lovelysystems.gradle"

gradlePlugin {
    website.set("https://github.com/lovelysystems/lovely-gradle-plugin")
    vcsUrl.set("https://github.com/lovelysystems/lovely-gradle-plugin")

    plugins {
        create(pluginId) {
            description = "Gradle Plugins for Lovely Systems Projects"
            tags.set(listOf("git", "docker"))

            id = pluginId
            implementationClass = "com.lovelysystems.gradle.LovelyGradlePlugin"
            displayName = "Lovely Systems Project Helpers"
            description = "Gradle Plugin for Lovely Systems Projects"
        }
    }
}

dependencies {
    listOf("s3", "s3-transfer-manager", "sso", "ssooidc").forEach {
        implementation("software.amazon.awssdk:$it:2.26.30")
    }
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.amshove.kluent:kluent:1.72")
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

plugins.withType<JavaPlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
