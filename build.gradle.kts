import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    java
    id("org.jetbrains.intellij.platform")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(providers.gradleProperty("javaToolchain").orElse("25").get().toInt())
    }
}
tasks.withType<JavaCompile>().configureEach { options.release = 21 }

dependencies {
    testImplementation("junit:junit:4.13.2")
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = providers.gradleProperty("pluginId")
        name = providers.gradleProperty("pluginName")
        version = project.version.toString()
        ideaVersion { sinceBuild = "262" }
    }
    pluginVerification { ides { recommended() } }
}

val serverName = providers.gradleProperty("serverBinary")
val windows = System.getProperty("os.name").startsWith("Windows")
val serverExecutable = serverName.map { it + if (windows) ".exe" else "" }
val cargoOutput = layout.buildDirectory.dir("cargo")
val cargoCommand = providers.environmentVariable("CARGO").orElse("cargo")

val buildServer by tasks.registering(Exec::class) {
    group = "build"
    description = "Build the native Rust server for the current host."
    workingDir("server")
    inputs.files("server/Cargo.toml", "server/Cargo.lock", "server/rust-toolchain.toml")
    inputs.files(fileTree("server/src") { include("**/*.rs") })
    outputs.dir(cargoOutput.map { it.dir("release") })
    environment("CARGO_TARGET_DIR", cargoOutput.get().asFile.absolutePath)
    commandLine(cargoCommand.get(), "build", "--release", "--locked", "--bin", serverName.get())
}

val bundledPluginDirectoryName = project.name
tasks.withType<PrepareSandboxTask>().configureEach {
    dependsOn(buildServer)
    from(cargoOutput.map { it.file("release/" + serverExecutable.get()) }) {
        into("$bundledPluginDirectoryName/server")
        filePermissions { unix("rwxr-xr-x") }
    }
}

val generateServerMetadata by tasks.registering {
    val output = layout.buildDirectory.dir("generated/serverMetadata")
    inputs.property("serverBinary", serverExecutable)
    inputs.property("pluginId", providers.gradleProperty("pluginId"))
    inputs.property("pluginName", providers.gradleProperty("pluginName"))
    outputs.dir(output)
    val binary = serverExecutable
    val id = providers.gradleProperty("pluginId")
    val name = providers.gradleProperty("pluginName")
    doLast {
        val file = output.get().file("lspf-server.properties").asFile
        file.parentFile.mkdirs()
        // Escape the .properties delimiters; the plugin reads this file back as UTF-8.
        fun escape(value: String) =
            value.replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=")
        file.writeText(
            "binary=" + escape(binary.get()) +
                "\npluginId=" + escape(id.get()) +
                "\npluginName=" + escape(name.get()) + "\n"
        )
    }
}
sourceSets {
    main {
        java.setSrcDirs(listOf("plugin/src/main/java"))
        resources.setSrcDirs(listOf("plugin/src/main/resources"))
        resources.srcDir(generateServerMetadata)
    }
    test {
        java.setSrcDirs(listOf("plugin/src/test/java"))
        resources.setSrcDirs(emptyList<String>())
    }
}

tasks.register<Exec>("testServer") {
    group = "verification"
    workingDir("server")
    commandLine(cargoCommand.get(), "test", "--locked")
}
tasks.named("check") { dependsOn("testServer") }
