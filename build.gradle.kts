import org.gradle.internal.impldep.com.amazonaws.auth.AWSCredentials

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.2.20"
}

repositories {
    jcenter()
}

group = "com.lovelysystems"
version = "0.0.2"



gradlePlugin {
    (plugins) {
        "lovely" {
            id = "lovely-gradle-plugin"
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
        maven {
            url = uri("s3://lovelymaven/")
            credentials(AwsCredentials::class.java) {
                accessKey = System.getenv("AWS_ACCESS_KEY_ID")
                secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")
            }
        }
    }

    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
        }
    }
}

task(name = "wrapper", type = Wrapper::class) {
    gradleVersion = "4.5"
}