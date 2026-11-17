pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NewPipeExtractor releases are distributed through JitPack (Phase 2).
        maven("https://jitpack.io")
    }
}

rootProject.name = "Grabi"
include(":app")