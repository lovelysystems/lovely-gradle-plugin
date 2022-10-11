# Lovely Gradle Plugin

[![Build Status](https://travis-ci.org/lovelysystems/lovely-gradle-plugin.svg?branch=master)](https://travis-ci.org/lovelysystems/lovely-gradle-plugin)
[![Gradle Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/lovelysystems/gradle/com.lovelysystems.gradle.gradle.plugin/maven-metadata.xml.svg?label=gradle-plugin)](https://plugins.gradle.org/plugin/com.lovelysystems.gradle)

This repository provides a Gradle plugin used by Lovely Systems Projects. It is only tested with the
[kotlin-dsl](https://github.com/gradle/kotlin-dsl) for Gradle, Groovy is not supported.

The newest version of this plugin can be found on
the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.lovelysystems.gradle)
and can be applied to your project by adding it to the plugins section of your `build.gradl.kts`
file like this:

```
plugins {
    id("com.lovelysystems.gradle") version ("1.0.0")
}
```

## Git Project

The Git project support automatically sets the
[project version](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:version)
from the Git state of the project by using `git describe` internally. It also allows to
automatically generate a new version tag and push it based on the changelog.

To enable this functionality add the following to your `build.gradle.kts` file:

```gradle
lovely {
  gitProject()
}
```

### Tasks

* `printVersion`: Prints out the current version of the project
* `printChangeLogVersion`: Parses the changes file and prints out the latest defined version
* `createTag` - Creates a new git tag for the current version and pushes the tag to the upstream

The `createTag` task creates a version tag for the latest version defined in the `CHANGES.rst` file
in the root of the project. The task validates the state of the project tree and only allows to tag
the version if the validation passes. The following conditions need to be met in order
for `createTag` to operate:

- The latest changelog entry in the changes file needs to have a valid version number (e.g: 0.1.2).
- There are no uncommitted changes or unknown files in the work tree.
- Your current branch is the default branch or a release branch (e.g: `release/0.1`)
- The same or a newer git tag does not exist.

## Docker Project

> INFO: Requires Docker >= 20.10.1 with buildx >= v0.5.0

This functionality is enabled by defining the docker repository for the project in the
`build.gradle.kts` file like this::

```gradle
lovely {
  dockerProject("hub.example.com/lovely/exampleproject")
}
```

As for any docker repository the hostname of the registry is not required and defaults
to ``docker.io``

Optionally a list of stages to build can be supplied to build multiple stages from a single
Dockerfile. The default is to just build the default stage, which is actually an empty string, so
the above example naming the stage explicitly looks like this:

```gradle
lovely {
  dockerProject("hub.example.com/lovely/exampleproject", stages=listOf(""))
}
```

See the [example Project](./example/build.gradle.kts) to see stage builds in action.

### Platforms

By default, docker images are pushed for target platforms `linux/amd64` and `linux/arm64` (e.g. Apple M1).
If not pushed, the image is build and loaded to the container registry for the platform only that runs the build.
However, the target platforms can be configured by passing a list of
supported [platform identifiers](https://docs.docker.com/engine/reference/commandline/buildx_build/#platform).
In case there are build platforms set, the local docker builds the image for these platforms. Otherwise the
local system architecture is used as build platform.

```gradle
lovely {
  dockerProject(
    "hub.example.com/lovely/exampleproject", 
    platforms = listOf("linux/amd64", "linux/arm/v7"),
    buildPlatforms = listOf("linux/amd64")
  )
}
```

The `buildDockerImage` task use [BuildKit](https://docs.docker.com/build/buildx/) in order to build
Docker images for different target platforms. In order to run the following build tasks you need to create the
docker-container builder on your host.

> Note: Run `./gradlew prepareDockerContainerBuilder` or `./gradlew pDCB` in order to bootstrap the required
> docker-container builder that supports the enhanced buildx features.

### Tasks

* `printDockerTag` - Prints out the currently generated Docker tag
* `prepareDockerImage` - Prepares all files required for a Docker build
* `buildDockerImage` - Builds a Docker image and tags it with current version and dev
* `pushDockerImage` - Pushes the Docker image to the registry
* `pushDockerDevImage` - Pushes the Docker image to the registry and tag it as `dev`

## Python Project

The Python project support ads an opinionated project setup for Python projects with the use of
[pip-tools](https://github.com/jazzband/pip-tools). To enable it add the following to
your `build.gradle.kts` file:

```gradle
lovely {
  pythonProject()
}
```

## License

This plugin is made available under
the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

