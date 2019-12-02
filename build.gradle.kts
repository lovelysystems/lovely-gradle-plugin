plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.3.50"
    id("com.gradle.plugin-publish") version "0.10.1"
}

repositories {
    jcenter()
}

group = "com.lovelysystems"
version = "1.2.0"

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
    testImplementation("org.amshove.kluent:kluent:1.35")
}

publishing {
    repositories {
        maven(url = buildDir.resolve("repository")) {
            name = "test"
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }

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
