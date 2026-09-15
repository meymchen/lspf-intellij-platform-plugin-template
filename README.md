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

**Fork, clone, or download the source archive.** The workflow deliberately skips
forks, which keep their own name, so run the script yourself from any directory:

```shell
python3 .github/template-cleanup/cleanup.py owner/my-lsp-plugin
```

Use `python` or `py -3` on Windows. The script only edits and moves files, so it
needs neither Git history nor a remote, and it works the same in an extracted
archive. Until it has run, `buildPlugin` warns that the build still carries the
template's plugin ID.

The language-specific work stays yours. Every place it waits for you carries a
`TODO(template)` marker, so `git grep -n "TODO(template)"` lists the whole set:

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
  src/main/java/   JetBrains LSP provider, settings page, and server lookup
  src/main/resources/META-INF/plugin.xml
  src/test/java/   Plugin integration and executable lookup tests
examples/          Sample .hello document
build.gradle.kts   Plugin build and server packaging
```

The Rust crate is independent of the plugin. Gradle builds it with Cargo
and copies its executable into the plugin distribution.

## Run locally

Install JDK 25 and Rust 1.98 or later through rustup. Put Java and Cargo on
PATH, or set JAVA_HOME and CARGO. The Gradle toolchain defaults to Java 25,
and the plugin targets that same release: IntelliJ IDEA 2026.2 is compiled for
Java 25 and bundles a Java 25 runtime, so no IDE that can load this plugin runs
on anything older. The `javaToolchain` property moves both.

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
an informational diagnostic.

The plugin appears in the Language Services widget in the status bar, with its
own icon and a link to its settings page. That page also sits under
**Settings > Tools**, named after the plugin, and holds three things: whether
the plugin starts a server at all, which executable it starts, and how much
that server logs. Applying a change restarts the running servers, which is also
how a rebuilt server takes effect without restarting the IDE.

## Point the sandbox at another server

`runIde` starts the server bundled into the sandbox plugin. Two Gradle
properties redirect that while you are working on the server itself:

```powershell
.\gradlew.bat runIde -PserverPath=build/cargo/debug/lspf-hello.exe -PserverLog=debug
```

`serverPath` names any server executable — a `cargo build` debug binary, or one
from a separate checkout — and a relative path resolves against this repository.
`serverLog` becomes the server's `RUST_LOG` filter, so `debug` or `lspf=trace`
reaches the tracing subscriber in `server/src/log_format.rs`. Gradle still
builds and bundles the release server; these only change which executable the
plugin starts.

Gradle passes both as system properties of the sandbox IDE:
`lspf-hello.server.path` and `lspf-hello.server.log`. Outside Gradle, set those
in **Help > Edit Custom VM Options**, or use the environment variables
`LSPF_HELLO_SERVER_PATH` and `LSPF_HELLO_SERVER_LOG`. The names follow
`serverBinary` in `gradle.properties`, so a renamed copy of this template gets
its own and two plugins never read each other's.

Three sources answer each question, in this order: the settings page, then the
system property, then the environment variable. The settings page comes first
because it is the one a user can see — leave a field empty to fall back. All
three belong to the IDE, which is why an opened project can never select the
executable that the plugin runs.

## Troubleshooting

Add `#com.intellij.platform.lsp` to **Help > Diagnostic Tools > Debug Log
Settings** in the sandbox IDE. The IDE then records the LSP session — the
command it started, the initialize handshake, and the traffic in both
directions — in `idea.log`, which **Help > Show Log** reveals in your file
manager.

The server keeps its own log on stderr so that stdout stays a valid LSP byte
stream. `-PserverLog=debug` raises its level. To read that log without the IDE
in the way, run the server directly:

```powershell
cd server
cargo run
```

It then waits for LSP messages on stdin, and `cargo test` drives the same
executable through a scripted session.

A server that fails to start reports the reason through the plugin: a missing
*bundled* executable means the installed package was built for another operating
system, while a missing *configured* executable means `serverPath` or
`LSPF_HELLO_SERVER_PATH` points at something that is not there.

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

## What this template leaves to you

The example is complete as an example. Four things a real plugin needs are
deliberately absent, because each one is a decision rather than a default.

**One package per operating system.** The ZIP carries the server built on the
machine that produced it, and `ServerBinary.resolveBundled` looks in exactly one
place, `server/<executable>`. A Marketplace upload is a single file that has to
serve every platform, so publishing there means adopting a per-platform layout
such as `server/<os>-<arch>/<executable>`, teaching `resolveBundled` to pick the
running host, and adding a release job that merges the three CI artifacts into
one archive. Until then, hand out the per-OS ZIPs that CI already produces,
labelled by platform.

**A release path.** `verifyPlugin` runs the IntelliJ Plugin Verifier against
whole IDEs. It is configured in `build.gradle.kts` but kept out of `check`,
because it downloads them; run it before you publish. Signing and uploading need
`signing` and `publishing` blocks in the `intellijPlatform` extension, a
Marketplace token and a certificate in repository secrets, and a
`META-INF/pluginIcon.svg`, which this template does not carry. Change notes want
a changelog to generate them from.

**A file type of your own.** The plugin claims files by extension, which is all
the LSP integration needs, but the IDE still has no file type for them: no
highlighting while the server is starting or stopped, no comment action, no icon
in the project tree. Registering a `FileType` and a `Language` is separate work
from the LSP wiring, and it is what makes the files feel like a supported
language rather than plain text with annotations.

**Knowing which features the IDE consumes.** A capability your server advertises
does nothing until the platform supports it, and the supported set grows with
each release; the
[LSP documentation](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html)
is the current list. `LspClientDescriptor.lspCustomization` is where a plugin
narrows or extends that set, and it is the first place to look when a correct
server produces nothing in the editor — on-type formatting, for one, is off
unless a plugin turns it on. This template does not override it.

## License and sources

Licensed under [Apache-2.0](LICENSE), matching the JetBrains plugin template.
The Rust example is used under its upstream Apache-2.0 option.
See [UPSTREAM.md](UPSTREAM.md) for the source revisions.
