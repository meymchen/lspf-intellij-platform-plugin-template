import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
    java
    id("org.jetbrains.intellij.platform")
}

// IntelliJ Platform 2026.2 is compiled for Java 25 and its IDEs bundle a Java 25
// runtime, so the toolchain reads the platform classes and the plugin targets the
// same release. Overriding javaToolchain moves both together.
val javaVersion = providers.gradleProperty("javaToolchain").orElse("25").map(String::toInt)
java {
    toolchain { languageVersion = javaVersion.map(JavaLanguageVersion::of) }
}
tasks.withType<JavaCompile>().configureEach { options.release = javaVersion }

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

// The sandbox copy above is executable on disk, but the distribution archive is written
// with uniform file permissions, so the entry has to be marked again. Without this the
// installed plugin cannot start its own server on Linux or macOS.
tasks.named<Zip>("buildPlugin") {
    filesMatching("**/server/*") { permissions { unix("rwxr-xr-x") } }
}

val generateServerMetadata = tasks.register("generateServerMetadata") {
    val output = layout.buildDirectory.dir("generated/serverMetadata")
    inputs.property("serverBinary", serverExecutable)
    inputs.property("serverName", serverName)
    inputs.property("pluginId", providers.gradleProperty("pluginId"))
    inputs.property("pluginName", providers.gradleProperty("pluginName"))
    inputs.property("fileExtension", providers.gradleProperty("fileExtension"))
    inputs.property("languageId", providers.gradleProperty("languageId"))
    outputs.dir(output)
    val binary = serverExecutable
    val plain = serverName
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
                // The name without the .exe suffix; it names the developer overrides.
                "\nserverName=" + escape(plain.get()) +
                "\npluginId=" + escape(id.get()) +
                "\npluginName=" + escape(name.get()) +
                // What the plugin claims in the IDE, and what it calls it on the wire.
                "\nfileExtension=" + escape(extension.get()) +
                "\nlanguageId=" + escape(language.get()) + "\n"
        )
    }
}
// Developer overrides for the sandbox IDE, passed as its own system properties:
//   ./gradlew runIde -PserverPath=build/cargo/debug/<serverBinary> -PserverLog=debug
// The plugin launches the named executable instead of the bundled one, and hands
// the filter to the server as RUST_LOG. Both are optional and change nothing else.
val serverPathOverride = providers.gradleProperty("serverPath")
val serverLogOverride = providers.gradleProperty("serverLog")
tasks.withType<RunIdeTask>().configureEach {
    // Resolve to plain strings here; the provider itself must not cross into the
    // argument provider, which Gradle evaluates after the configuration cache.
    val prefix = serverName.get()
    val path = serverPathOverride.orNull?.let { file(it).absolutePath }
    val filter = serverLogOverride.orNull
    jvmArgumentProviders += CommandLineArgumentProvider {
        buildList {
            path?.let { add("-D$prefix.server.path=$it") }
            filter?.let { add("-D$prefix.server.log=$it") }
        }
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
// verifyPluginStructure reads plugin.xml the way the platform does and takes seconds.
// Two verifications stay out: verifyPlugin downloads whole IDEs and belongs to a
// release, and verifyPluginProjectConfiguration reports a Java version mismatch in
// either direction, because the build table in this plugin version predates 2026.2.
tasks.named("check") { dependsOn("testServer", "verifyPluginStructure") }
