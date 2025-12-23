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
    versionCatalogs {
        create("shared") {
            from(files("../../gradle/libs.versions.toml"))
        }
        create("appLocal") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Drawmark Android(Prototype"
include(":app")

// Include the android-lib library from libs folder
includeBuild("../../libs/android-lib") {
    dependencySubstitution {
        substitute(module("app.drawmark.android:lib")).using(project(":"))
    }
}
