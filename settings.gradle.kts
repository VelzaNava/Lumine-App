pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Required for unityandroidpermissions.aar inside unityLibrary/libs
        flatDir {
            dirs("${rootProject.projectDir}/unityLibrary/libs")
        }
    }
}

rootProject.name = "LumineApp"
include(":app")
include(":unityLibrary")
// Unity's XR manifest is a submodule of unityLibrary
include(":unityLibrary:xrmanifest.androidlib")