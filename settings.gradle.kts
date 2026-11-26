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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.weliem")
            }
        }
    }
}

rootProject.name = "OpenCult"

include(
    ":app",
    ":core:designsystem",
    ":core:bia",
    ":core:ble",
    ":core:audio",
    ":core:database",
    ":core:healthconnect",
    ":core:sync-supabase",
    ":feature:onboarding",
    ":feature:home",
    ":feature:weighin",
    ":feature:bodymap",
    ":feature:reports",
    ":feature:profiles",
    ":feature:inspector",
)
