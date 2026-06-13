rootProject.name = "ecommerce-monolith-java"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "shared-kernel",
    "product",
    "payment",
    "inventory",
    "infrastructure",
    "app",
)
