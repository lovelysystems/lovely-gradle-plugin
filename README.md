# Lovely Gradle Plugin

This repository provides a Gradle plugin used by Lovely Systems Projects. It is only tested with the
 [kotlin-dsl](https://github.com/gradle/kotlin-dsl) for Gradle, Groovy is not supported.

The newest version of this plugin can be found on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.lovelysystems.gradle)
and can be applied to your project by adding it to the plugins section of your `build.gradl.kts` file like this:

```
plugins {
    id("com.lovelysystems.gradle") version ("0.0.3")
}
```

## Git Project

The Git project support automatically sets the
[project version](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:version)
from the Git state of the project by using `git describe` internally. It also allows to automatically generate a new version
tag and push it based on the changelog.

To enable this functionality add the following to your `build.gradle.kts` file:

```gradle
lovely {
  gitProject()
}
```

### Tasks

  * `printVersion`: Prints out the current version of the project
  * `createTag` - Creates a new git tag for the current version and pushes the tag to the upstream

The `createTag` task creates a version tag for the latest version defined in the `CHANGES.rst` file
in the root of the project. The task validates the state of the project tree and only allows to tag the version
if the validation passes. The following conditions need to be met in order for `createTag` to operate:

 - The latest changelog entry in the changes file needs to have a valid version number (e.g: 0.1.2).
 - There are no uncommitted changes or unknown files in the work tree.
 - Your current branch is `master` and is in sync with the upstream `master` branch head.
 - The same or a newer git tag does not exist.

## Docker Project

To enable this functionality add the following to your `build.gradle.kts` file:

```gradle
lovely {
  dockerProject("some.hub.com")
}
```

The above statement uses `some.hub.com` as the Docker registry to push the images to.

### Tasks

  * `printDockerTag` - Prints out the currently generated Docker tag
  * `prepareDockerImage` - Prepares all files required for a Docker build
  * `buildDockerImage` - Builds a Docker image and tags it with current version and dev
  * `pushDockerImage` - Pushes the Docker image to the registry
  * `pushDockerDevImage` - Pushes the Docker image to the registry and tag it as `dev`

## Limitations

Currently only [Clones with SSH URLs](https://help.github.com/articles/which-remote-url-should-i-use/#cloning-with-ssh-urls)
are supported. Using https clones will result in an exception like

```
> org.eclipse.jgit.api.errors.TransportException: https://github.com/org/repo.git:
Authentication is required but no CredentialsProvider has been registered
```


## License

This plugin is made available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

