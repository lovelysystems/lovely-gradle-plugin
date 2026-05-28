# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Gradle plugin (`com.lovelysystems.gradle`) used by Lovely Systems projects. Provides opinionated
conventions for git versioning, Docker builds, Python project management, and AWS SSO integration.
Kotlin-DSL only — Groovy is not supported.

Published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.lovelysystems.gradle).

## Build & Test

```shell
./gradlew test              # run all tests
./gradlew test --tests 'com.lovelysystems.gradle.VersionTest'  # single test class
./gradlew test --tests '*.VersionTest.testVersions'            # single test method
```

CI runs on CircleCI with a `machine` executor (Docker tests need a real Docker daemon).

## Publishing

See [DEVELOP.md](DEVELOP.md). Requires `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` env vars.
Version must be set in both `CHANGES.md` and `build.gradle.kts` before tagging and publishing.

## Local development in consumer projects

Add to the consumer's `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("../lovely-gradle-plugin/")
}
```

## Architecture

The plugin registers a `lovely { }` extension (`LovelyPluginExtension`) that gates each subsystem:

- **`gitProject()`** — `LSGit.kt` / `LovelyGradlePlugin.kt`
  Sets project version from `git describe`. Registers `printVersion`, `printChangeLogVersion`,
  `createTag` tasks. Changelog parsing (`ChangeLog.kt`) supports `.md`, `.rst`, `.txt` formats
  with pattern `## YYYY-MM-DD / MAJOR.MINOR.PATCH`.

- **`dockerProject(repo, stages, platforms, buildPlatforms)`** — `LSDocker.kt`
  Multi-stage, multi-platform Docker builds via `docker buildx`. Registers `prepareDockerImage`,
  `buildDockerImage`, `pushDockerImage`, `pushDockerDevImage` tasks. Requires a
  `lovely-docker-container-builder` buildx builder.

- **`pythonProject(executable)`** — `PythonProject.kt`
  Manages a venv at `./v/`, pip-compile/pip-sync workflow. Registers `venv`, `dev`, `test`,
  `writeVersion`, `sdist` tasks. Converts git-describe versions to PEP 440.

- **`awsProject(profile, region)`** — `LSAws.kt`
  AWS SSO credential fetching via CLI. Registers `ssoCredentials` task. Also provides
  `S3UploadDirectory` and `S3DownloadFile` task types (in separate files) using the AWS SDK v2.

### Key types

- `Version` — semver with optional revision (`MAJOR.MINOR.PATCH[-REVISION]`), implements
  `Comparable`.
- `LSGit` — wraps git CLI commands, handles tag creation/validation, branch validation
  (only `master`/`main` or `release/*` branches allowed for tagging).

## Conventions

- Target JVM 11.
- Tests use Gradle TestKit with `TemporaryFolder` (JUnit 4 rules) and Kluent assertions.
- `Testing.kt` has shared helpers (`createSampleRepos`, `createVersionedFile`) for git-based tests.
- Version format: `MAJOR.MINOR.PATCH` with optional `-REVISION` suffix.
- Changelog format: `## YYYY-MM-DD / X.Y.Z` headings in `CHANGES.md`.
