import org.gradle.internal.impldep.aQute.bnd.plugin.gradle.GradlePlugin
import org.gradle.internal.impldep.com.amazonaws.auth.AWSCredentials

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.2.20"
    id("com.gradle.plugin-publish") version "0.9.9"
}

repositories {
    jcenter()
}

group = "com.lovelysystems"
version = "0.0.2"

val pluginId = "lovely-gradle-plugin"

gradlePlugin {
    (plugins) {
        pluginId {
            id = pluginId
            implementationClass = "com.lovelysystems.gradle.LovelyGradlePlugin"
        }
    }
}

dependencies {
    compile("org.eclipse.jgit:org.eclipse.jgit:4.10.0.201712302008-r")
    compile("com.jcraft:jzlib:1.1.3")
    testCompile(gradleTestKit())
    testCompile("junit:junit:4.12")
    testCompile(kotlin("test"))
    testCompile(kotlin("test-junit"))
    testCompile("org.amshove.kluent:kluent:1.32")
}

publishing {
    repositories {
        maven(url = buildDir.resolve("repository")) {
            name = "test"
        }
    }
    (publications) {
        "mavenJava"(MavenPublication::class) {
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
            id = pluginId
            displayName = "Lovely Systems Project Helpers"
            description = "Gradle Plugin for Lovely Systems Projects"
        }
    }
}

task(name = "wrapper", type = Wrapper::class) {
    gradleVersion = "4.5"
}
