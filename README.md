Lovely Gradle Plugin
====================

This repository provides a Gradle plugin used by Lovely Systems Projects. Examples provided
here are using the gradle [kotlin-dsl](https://github.com/gradle/kotlin-dsl)

Apply the plugin using standard gradle convention in your `build.gradle.kts` file:

````gradle
plugins {
  id("lovely-gradle-plugin") version ("0.0.1")
}
````

Currently the plugin artifacts are only published to an intermediate S3 bucket, therefore
it is required to add the following to your `settings.gradle.kts` file:

````gradle
pluginManagement {
    repositories {
        maven {
            url = uri("http://lovelymaven.s3.eu-central-1.amazonaws.com/")
        }
        gradlePluginPortal()
    }
}
````

Git Project
-----------

The Git project support automatically sets the project version from the Git
state of the project. It also allows to automatically generate a new version
tag and push it based on the changelog.

To enable this functionality add the following to your `build.gradle.kts` file:

````gradle
lovely {
  gitProject()
}
````

### Tasks

  * `printVersion`: prints out the version of the project
  * `createTag` - Creates a new git tag for the current version and pushes the tag to the upstream

The `createTag` task validates the state of the current work tree and only allows to tag the version
if the validation passes.

Docker Project
--------------

To enable this functionality add the following to your `build.gradle.kts` file:

````gradle
lovely {
  dockerProject("some.hub.com")
}
````

The above statement uses `some.hub.com` as the docker registry to push the images to.

### Tasks

  * `prepareDockerImage` - Prepares all files required for a Docker build
  * `buildDockerDevImage` - Builds a docker image and tags it with current version and dev
  * `pushDockerDevImage` - Pushes the docker image to the registry


License
-------
This plugin is made available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

