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

// --- template identity check; the template cleanup removes this block ---
// A copy that skipped the cleanup would ship the template's own plugin ID and
// collide with it, so warn about it before the distribution is assembled.
val templatePluginId = "io.github.meymchen.lspf.hello"
val templateIdentityWarning =
    "This build still uses the template plugin ID $templatePluginId. Run " +
        "'python3 .github/template-cleanup/cleanup.py <owner>/<repository>' first."
val checkTemplateIdentity = tasks.register("checkTemplateIdentity") {
    // Only local values may cross into the task action; capturing a script
    // property would make the task unserializable for the configuration cache.
    val configured = providers.gradleProperty("pluginId")
    val template = templatePluginId
    val warning = templateIdentityWarning
    doLast {
        if (configured.get() == template) {
            println(warning)
        }
    }
}
tasks.named("buildPlugin") { dependsOn(checkTemplateIdentity) }
// --- end template identity check ---

val serverName = providers.gradleProperty("serverBinary")
val windows = System.getProperty("os.name").startsWith("Windows")
val serverExecutable = serverName.map { it + if (windows) ".exe" else "" }
val cargoOutput = layout.buildDirectory.dir("cargo")
val cargoCommand = providers.environmentVariable("CARGO").orElse("cargo")

val buildServer = tasks.register<Exec>("buildServer") {
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

val generateServerMetadata = tasks.register("generateServerMetadata") {
    val output = layout.buildDirectory.dir("generated/serverMetadata")
    inputs.property("serverBinary", serverExecutable)
    inputs.property("pluginId", providers.gradleProperty("pluginId"))
    inputs.property("pluginName", providers.gradleProperty("pluginName"))
    inputs.property("fileExtension", providers.gradleProperty("fileExtension"))
    inputs.property("languageId", providers.gradleProperty("languageId"))
    outputs.dir(output)
    val binary = serverExecutable
    val id = providers.gradleProperty("pluginId")
    val name = providers.gradleProperty("pluginName")
    val extension = providers.gradleProperty("fileExtension")
    val language = providers.gradleProperty("languageId")
    doLast {
        val file = output.get().file("lspf-server.properties").asFile
        file.parentFile.mkdirs()
        // Escape the .properties delimiters; the plugin reads this file back as UTF-8.
        fun escape(value: String) =
            value.replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=")
        file.writeText(
            "binary=" + escape(binary.get()) +
                "\npluginId=" + escape(id.get()) +
                "\npluginName=" + escape(name.get()) +
                // What the plugin claims in the IDE, and what it calls it on the wire.
                "\nfileExtension=" + escape(extension.get()) +
                "\nlanguageId=" + escape(language.get()) + "\n"
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
