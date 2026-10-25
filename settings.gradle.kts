rootProject.name = "kotlin_calendar"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":server")
include(":client:shared")
include(":client:androidApp")
include(":client:desktopApp")
include(":client:webApp")
