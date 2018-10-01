# Changes for Lovely Gradle Plugin

## unreleased

- allow patch updates in createTag for older releases

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
