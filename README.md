# Lovely Gradle Plugin

This repository provides a Gradle plugin used by Lovely Systems Projects. Examples provided
here are using the gradle [kotlin-dsl](https://github.com/gradle/kotlin-dsl)

Apply the plugin using standard gradle convention  by following the instructions shown
on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.lovelysystems.gradle).

## Git Project

The Git project support automatically sets the project version from the Git
state of the project. It also allows to automatically generate a new version
tag and push it based on the changelog.

To enable this functionality add the following to your `build.gradle.kts` file:

```gradle
lovely {
  gitProject()
}
```

### Tasks

  * `printVersion`: Prints out the version of the project
  * `createTag` - Creates a new git tag for the current version and pushes the tag to the upstream

The `createTag` task validates the state of the current work tree and only allows to tag the version
if the validation passes.

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

