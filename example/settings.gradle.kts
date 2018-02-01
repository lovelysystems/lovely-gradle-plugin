// use the local maven repository of the parent project
pluginManagement {
    repositories {
        maven ("../build/repository")
        gradlePluginPortal()
    }
}
