# Changes for Lovely Gradle Plugin

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
