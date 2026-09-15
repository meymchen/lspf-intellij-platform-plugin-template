import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    // The portal stays first and serves the plugin markers. Maven Central is a fallback
    // for the plugins' own dependencies, which the portal proxies and has failed to
    // serve here often enough to break a build that had nothing else wrong with it.
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}
rootProject.name = "lspf-intellij-platform-plugin-template"
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform { defaultRepositories() }
    }
}
