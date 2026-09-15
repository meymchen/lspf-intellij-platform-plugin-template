import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories { gradlePluginPortal() }
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
