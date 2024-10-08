# Changes for Lovely Gradle Plugin

## 2024-09-16 / 1.15.1

### Fix

- to be able to use downloadFile in ci, also get aws credentials from environment

## 2024-09-13 / 1.15.0

### Features

- add `S3DownloadFile` task
- added optional properties `regionOverride` and `profileOverride` to `S3UploadDirectory`  
- deprecated properties `region` and `profile` on `S3UploadDirectory` task

### Development

- Upgrade examples project to gradle 8.8

## 2024-08-22 / 1.14.2

### Fix

- use default `S3AsyncClient` instead of `S3CrtAsyncClient` (has issues with Content-Encoding header,
  see [issue](https://github.com/aws/aws-sdk-java-v2/issues/5071))

## 2024-08-22 / 1.14.1

### Fix

- set the proper content type per file when uploading files to S3 using `S3UploadDirectory` task type
- make AWS region configurable in `S3UploadDirectory` task type instead of using a fixed value

## 2024-08-20 / 1.14.0

### Feature

- introduce `LSAws` plugin to setup and retrieve AWS SSO credentials

## 2024-06-20 / 1.13.0

### Feature

- support optional `platform` property in the `DockerCopy` task to use a specific platform for creating the source container

### Develop

- upgraded to kotlin 2.0.0 and gradle 8.8
- minor code style changes

## 2023-08-23 / 1.12.0

### Develop

- upgraded to kotlin 1.9 and gradle 1.8.x

### Fixes

- push multistage docker builds correctly, before only the base stage was pushed
  to all stage tags
- recognize already existing tags if platform is not the local platform

## 2023-07-19 / 1.11.5

### Fixes

- use pip-tools version 7.1.0 to solve issue with DEV_PKGS
  (https://pyup.io/packages/pypi/pip-tools/changelog?page=1&#7.0.0)

## 2023-02-01 / 1.11.4

### Fixes

- use pip-tools version 6.13.0 to work with pip >= 23.1
  (https://pyup.io/packages/pypi/pip-tools/changelog?page=1&#6.13.0)

## 2023-02-01 / 1.11.3

### Fixes

- make distribution compatible with java 11

## 2023-01-31 / 1.11.2

- Integrate with CircleCI and remove travis

## 2023-01-30 / 1.11.1

### Fixes

- pythonProject: use latest pip-tools and remove pins for setuptools and pip
- setuptools compatibility: convert git describe output to pep440 compatible version

## 2023-01-30 / 1.11.0

- nail setuptools < 66.0.0 for Python projects see https://github.com/pypa/setuptools/issues/3772
- updated Gradle (7.5.1 -> 7.6), Kotlin (1.4.31 -> 1.7.10) and other dependencies in the project

## 2022-12-06 / 1.10.0

- make all tasks lazy by switching to `tasks.register` instead of `tasks.create` (https://docs.gradle.org/current/userguide/task_configuration_avoidance.html)

### Breaking

- remove `dockerFiles` setting from `LovelyPluginExtension` to avoid initialization errors due to eager loading. instead use the added closure on the `dockerProject` function (see [example](https://github.com/lovelysystems/lovely-gradle-plugin/blob/master/example/build.gradle.kts))

## 2022-12-01 / 1.9.2

- fix: make `prepareDockerContainerBuilder` visible under gradle tasks

## 2022-08-29 / 1.9.1

- fix: avoid bootstrap of custom docker-container builder since this is not supported in
  older docker versions.

## 2022-08-29 / 1.9.0

- allow to specify build platforms for local docker builds.

## 2022-08-29 / 1.8.1

- fix: Prefer the usage of the custom docker-container builder for local builds
  in order to utilize BuildKit features.

## 2022-08-24 / 1.8.0

- Upgrade to gradle 7.5.1
- Build docker images with BuildKit. (Requires Docker >= 20.10.1)
- allow to customize target platforms of docker images.

## 2022-05-25 / 1.7.0

- do not verify on push of createTag

## 2021-12-16 / 1.6.2

- fix: pythonProject: do not delete sdist build when not executed

## 2021-10-12 / 1.6.1

- fix: re-enabled docker push, which was disabled for testing

## 2021-10-12 / 1.6.0

- allow to build, tag and push multiple stages from a single Dockerfile

## 2021-10-12 / 1.5.1

- nail pip to fix incompatibility with pip-tools

## 2021-08-16 / 1.5.0

- upgrade pip-tools to 6.2.0
- do not nail pip and setuptools in python venv

## 2021-08-03 / 1.4.0

- added the DockerCopy task, which allows to copy a file from a docker image

## 2020-08-05 / 1.3.2

- the `writeVersion` task now compares the contents of the VERSION.txt
  file to ensure, that always the most recent version is used (e.g: by sdist)

## 2020-08-04 / 1.3.1

- detect `/tests` as pytest root if there is a `tests/pytests.ini` file.

## 2020-03-26 / 1.3.0

- added PythonProject

## 2019-01-03 / 1.2.0

- upgrade to gradle 5.1

## 2018-10-04 / 1.1.0

- allow patch updates in createTag for older releases
- upgrade gradle to 4.10, older version won't work any more
- allow to push dev image even if the same build was pushed as release first

## 2018-08-11 / 1.0.0

- added printChangeLogVersion task
- additionally support CHANGES.txt as restructuredText changelog
- additionally support CHANGES.md as Markdown based changelog, which is now
  the preferred format

## 2018-08-08 / 0.0.7

- use gradle 4.9, fixes gradle 4.8+ failures in projects using this plugin

## 2018-03-22 / 0.0.6

- fixed an issue which causes git commands to hang in some setups
- fixed latest local release tag lookup, which failed when only none-release tags are present

## 2018-03-15 / 0.0.5

- allow release/x.x and default branch as target for createTag
- release versions can now have an optional revision (e.g: 1.2.3-1)
- fixed an issue that prevented git errors to be logged
- fixed website and vcs urls in meta data

## 2018-03-06 / 0.0.4

- fixed version ordering check which could have lead to false negatives
- git commands now use the local git installation instead of jgit

## 2018-02-01 / 0.0.3

- fixed a fetch issue with newer git repos

## 2018-02-01 / 0.0.2

- added example project
- allow to push the docker image without setting a dev tag

## 2018-01-30 / 0.0.1

- initial release
