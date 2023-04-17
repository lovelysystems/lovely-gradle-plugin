plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.7.10"
    id("com.gradle.plugin-publish") version "1.1.0"
}

repositories {
    mavenCentral()
}

group = "com.lovelysystems"
version = "1.11.4"

val pluginId = "com.lovelysystems.gradle"

gradlePlugin {
    plugins {
        create(pluginId) {
            id = pluginId
            implementationClass = "com.lovelysystems.gradle.LovelyGradlePlugin"
            displayName = "Lovely Systems Project Helpers"
            description = "Gradle Plugin for Lovely Systems Projects"
        }
    }
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.amshove.kluent:kluent:1.72")
}

pluginBundle {
    website = "https://github.com/lovelysystems/lovely-gradle-plugin"
    vcsUrl = "https://github.com/lovelysystems/lovely-gradle-plugin"
    description = "Gradle Plugins for Lovely Systems Projects"
    tags = listOf("git", "docker")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

plugins.withType<JavaPlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
