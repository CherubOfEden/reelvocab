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
    }
}

rootProject.name = "reelvocab"

include(":app")
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:today")
include(":feature:deck")
include(":feature:dictionary")
include(":feature:library")
include(":feature:settings")
include(":feature:media")
include(":feature:notes")
include(":feature:capture")
