# lspf IntelliJ Platform plugin template

A Rust language server built with [lspf](https://github.com/meymchen/lspf),
connected through the [JetBrains LSP API](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html).
The example provides diagnostics, hover, and completion for `.hello` files.

Requires a JetBrains IDE **2026.2 or later** with the `com.intellij.modules.lsp`
module. The build targets IntelliJ IDEA 2026.2 and declares minimum build 262.
See the [official list of supported IDEs](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html#supported-ides).

## Start a project from this template

However you take a copy, the first step is the same: rewrite the template's
identity, so that your plugin does not ship this repository's plugin ID. The
cleanup rewrites `group`, `pluginId`, `pluginName`, and `serverBinary` in
`gradle.properties`; `rootProject.name` in `settings.gradle.kts`; the ID, name,
vendor, and provider class in `plugin.xml`; the Java package directory; the Rust
package name in `server/Cargo.toml` and `server/Cargo.lock`; and the executable
referenced by `server/tests/protocol.rs`. It then removes itself and this section.

Names come from the repository slug: `owner/my-lsp-plugin` produces the package
`io.github.owner.mylspplugin`, the plugin name `My Lsp Plugin`, and the server
binary `my-lsp-plugin`.

**Use this template.** The
[cleanup workflow](.github/workflows/template-cleanup.yml) runs on the new
repository's default branch and commits the result. Creating a repository from a
template does not reliably emit a push event, so if no run appears, start
**Template cleanup** by hand from the **Actions** tab. The workflow skips this
template repository and forks, and a second run is a no-op.

**Clone, or download the source archive.** Run the script yourself, from any
directory:

```shell
python3 .github/template-cleanup/cleanup.py owner/my-lsp-plugin
```

Use `python` or `py -3` on Windows. The script only edits and moves files, so it
needs neither Git history nor a remote, and it works the same in an extracted
archive. Until it has run, `buildPlugin` warns that the build still carries the
template's plugin ID.

The language-specific work stays yours:

1. Set `fileExtension` and `languageId` in `gradle.properties`, and rename
   `examples/example.hello` to match. `fileExtension` takes one extension, or
   several separated by commas.
2. Set the vendor URL and description in
   `plugin/src/main/resources/META-INF/plugin.xml`.
3. Write the language server: replace the handlers in `server/src/main.rs` and
   extend `server/tests/protocol.rs`. See the
   [lspf documentation](https://github.com/meymchen/lspf).

`gradle.properties` is the single source for the plugin name and ID, the bundled
executable name, and the files the plugin claims. Gradle writes them into
`lspf-server.properties`, which the plugin reads at runtime, so the LSP service
display name, the launched command, and the file matching all follow it without a
second edit. No Java file names any of them, which is why nothing here needs
renaming.

## Layout

```text
server/
  Cargo.toml
  Cargo.lock
  src/             Rust language logic and stdio entry point
  tests/           LSP protocol integration tests
plugin/
  src/main/java/   JetBrains LSP provider and bundled server lookup
  src/main/resources/META-INF/plugin.xml
  src/test/java/   Plugin integration and executable lookup tests
examples/          Sample .hello document
build.gradle.kts   Plugin build and server packaging
```

The Rust crate is independent of the plugin. Gradle builds it with Cargo
and copies its executable into the plugin distribution.

## Run locally

Install JDK 25 and Rust 1.98 or later through rustup. Put Java and Cargo on
PATH, or set JAVA_HOME and CARGO. The Gradle toolchain defaults to Java 25
to read the IntelliJ IDEA 2026.2 platform classes. Generated plugin bytecode
still targets Java 21; this does not make the target IDE runnable on Java 21.

Open the repository as a Gradle project in IntelliJ IDEA. Select the shared
**Run plugin** configuration, or run:

```powershell
.\gradlew.bat runIde
```

Use `./gradlew` on Linux or macOS. Gradle downloads the target IDE, builds
the Rust server, and installs the plugin in its development sandbox.

In the sandbox IDE, use **File > Open** to open this repository's `examples`
directory as a project, then open `example.hello` from that project's tree.
The file must belong to the open project: opening it as an external file
while another project is active does not trigger the LSP startup callback.
Only `.hello` files start this example server; Markdown files are not supported.

Place the caret on `hello` and invoke **Quick Documentation** to see the server's
documentation. Invoke **Basic Completion** after typing `he` to see the `hello`
item with the detail `Example completion from lspf`. The server also publishes
an informational diagnostic. The IDE's Language Services widget provides LSP
connection status.

## Build and test

Run Rust checks independently from the server directory:

```powershell
cd server
cargo fmt --all -- --check
cargo clippy --locked --all-targets -- -D warnings
cargo test --locked
cd ..
```

Build and test the complete plugin from the repository root:

```powershell
.\gradlew.bat check buildPlugin
```

`check` runs the Java tests and the Rust protocol test. The protocol test
launches the real server and checks initialization, diagnostics, hover,
completion, shutdown, and exit. Plugin tests check file selection, language ID,
and the command used to start the bundled executable.

The ZIP in `build/distributions/` includes the native server under
`lspf-intellij-platform-plugin-template/server/`. Install it using
**Settings > Plugins > Install Plugin from Disk**.

Each ZIP targets the OS and architecture on which it was built. CI builds
separate Linux, Windows, and macOS artifacts and checks their server entries.
Before a Marketplace release, assemble and select binaries for every supported
target, or distribute explicitly labelled host packages.

## License and sources

Licensed under [Apache-2.0](LICENSE), matching the JetBrains plugin template.
The Rust example is used under its upstream Apache-2.0 option.
See [UPSTREAM.md](UPSTREAM.md) for the source revisions.
