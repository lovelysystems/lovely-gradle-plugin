plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.4.31"
    id("com.gradle.plugin-publish") version "0.15.0"
}

repositories {
    mavenCentral()
}

group = "com.lovelysystems"
version = "1.4.0"

val pluginId = "com.lovelysystems.gradle"

gradlePlugin {
    plugins {
        create(pluginId) {
            id = pluginId
            implementationClass = "com.lovelysystems.gradle.LovelyGradlePlugin"
        }
    }
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.12")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.amshove.kluent:kluent:1.68")
}

pluginBundle {
    website = "https://github.com/lovelysystems/lovely-gradle-plugin"
    vcsUrl = "https://github.com/lovelysystems/lovely-gradle-plugin"
    description = "Gradle Plugins for Lovely Systems Projects"
    tags = listOf("git", "docker")

    (plugins) {
        pluginId {
            displayName = "Lovely Systems Project Helpers"
            description = "Gradle Plugin for Lovely Systems Projects"
        }
    }
}
