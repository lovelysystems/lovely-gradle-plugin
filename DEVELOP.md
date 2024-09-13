# Development

## Include dev version of the plugin

To include an unpublished version of this plugin while making changes a local version of it can be included.
Add to `settings.gradle.kts` (Adapt the path as needed):

```kotlin
pluginManagement {
    includeBuild("../lovely-gradle-plugin/")
}
```

## Publish new version

To publish a new version to the gradle portal:

1. Create a new version. Set the version number in the [CHANGES.md](CHANGES.md) and the 
   [build.gradle.kts](build.gradle.kts) (`version = <new version number>`). Commit the changes. 

2. Tag the commit from step 1. with the version number. 

3. Configure the credentials for publishing to gradle portal. Set the environment variables:

```shell
export GRADLE_PUBLISH_KEY=...
export GRADLE_PUBLISH_SECRET=...
```

4. Runtask `./gradlew publishPlugins`
