#!/usr/bin/env python3
"""Rewrite the template's identity for a repository created from it.

Run it through the template-cleanup workflow or by hand from any directory:

    python3 .github/template-cleanup/cleanup.py owner/repository

It only touches files, so it works the same in a clone, in an extracted source
archive, and on a checkout inside a workflow.

Every replacement below is anchored to a known line so that unrelated
occurrences of the same words survive: the links to the upstream `lspf` crate
in README.md and the attribution in UPSTREAM.md must not be renamed.
"""

from __future__ import annotations

import os
import re
import shutil
import sys
from pathlib import Path

OLD_GROUP = "io.github.meymchen.lspf"
OLD_PLUGIN_ID = "io.github.meymchen.lspf.hello"
OLD_PACKAGE_PATH = "io/github/meymchen/lspf/hello"
OLD_PLUGIN_NAME = "LSPF Hello"
OLD_SERVER_BINARY = "lspf-hello"
OLD_ROOT_NAME = "lspf-intellij-platform-plugin-template"
OLD_VENDOR = "meymchen"

JAVA_SOURCE_ROOTS = ("plugin/src/main/java", "plugin/src/test/java")


def package_segment(value: str) -> str:
    """Reduce a GitHub owner or repository name to a legal Java package segment."""
    segment = re.sub(r"[^a-z0-9]", "", value.lower())
    if not segment or segment[0].isdigit():
        segment = "x" + segment
    return segment


def binary_name(value: str) -> str:
    """Reduce a repository name to a Cargo package name.

    The result also has to satisfy the allow-list in ServerBinary.resolve.
    """
    name = re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")
    return name or "language-server"


def display_name(value: str) -> str:
    words = [word for word in re.split(r"[^A-Za-z0-9]+", value) if word]
    return " ".join(word[:1].upper() + word[1:] for word in words) or value


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one {old!r}, found {count}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def replace_all(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"{path}: expected to find {old!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def move_package(root: Path, source_path: str, target_path: str) -> None:
    source = root / source_path
    if not source.is_dir():
        raise SystemExit(f"{source}: missing Java package directory")
    target = root / target_path
    if source == target:
        return
    # Stage through a sibling so that a target nested in the source's own
    # parent chain (owner/repo == meymchen/lspf) still moves cleanly.
    staged = root / "__package_move__"
    shutil.move(str(source), str(staged))
    for parent in source.parents:
        if parent == root or any(parent.iterdir()):
            break
        parent.rmdir()
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(staged), str(target))


NEXT_STEPS = """## Next steps

The template cleanup already applied this repository's name to `gradle.properties`,
`settings.gradle.kts`, `plugin.xml`, the Java package, and the Rust package. What
remains is language-specific:

1. Set `fileExtension` and `languageId` in `gradle.properties`, and rename
   `examples/example.hello` to match. `fileExtension` takes one extension, or
   several separated by commas.
2. Set the vendor URL and description in
   `plugin/src/main/resources/META-INF/plugin.xml`.
3. Write the language server: replace the handlers in `server/src/main.rs` and
   extend `server/tests/protocol.rs`. See the
   [lspf documentation](https://github.com/meymchen/lspf).

`gradle.properties` stays the single source for the plugin name and ID, the bundled
executable name, and the files the plugin claims: Gradle writes them into
`lspf-server.properties`, which the plugin reads at runtime. No Java file names any
of them, so nothing here needs renaming.

"""


def remove_identity_check(root: Path) -> None:
    """Drop the guard that warns while the template's plugin ID is configured."""
    path = root / "build.gradle.kts"
    text = path.read_text(encoding="utf-8")
    end_marker = "// --- end template identity check ---\n"
    start = text.index("// --- template identity check")
    end = text.index(end_marker, start) + len(end_marker)
    path.write_text(text[:start] + text[end:].lstrip("\n"), encoding="utf-8")


def rewrite_readme(root: Path, name: str, repository: str) -> None:
    path = root / "README.md"
    text = path.read_text(encoding="utf-8")

    text = text.replace(
        "# lspf IntelliJ Platform plugin template\n",
        f"# {name}\n",
        1,
    )
    text = text.replace(
        f"`{OLD_ROOT_NAME}/server/`",
        f"`{repository}/server/`",
        1,
    )

    start = text.index("## Start a project from this template")
    end = text.index("## Layout", start)
    text = text[:start] + NEXT_STEPS + text[end:]

    path.write_text(text, encoding="utf-8")


def main() -> None:
    slug = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("GITHUB_REPOSITORY", "")
    if "/" not in slug:
        raise SystemExit("usage: cleanup.py <owner>/<repository>")
    owner, repository = slug.split("/", 1)

    # Locate the repository from this file rather than the working directory, so
    # the script can be started from anywhere.
    root = Path(__file__).resolve().parents[2]
    if not (root / "settings.gradle.kts").is_file():
        raise SystemExit(f"{root}: not a checkout of the template")
    group = f"io.github.{package_segment(owner)}"
    plugin_id = f"{group}.{package_segment(repository)}"
    package_path = plugin_id.replace(".", "/")
    plugin_name = display_name(repository)
    server_binary = binary_name(repository)

    print(f"group          {group}")
    print(f"pluginId       {plugin_id}")
    print(f"pluginName     {plugin_name}")
    print(f"serverBinary   {server_binary}")
    print(f"rootProject    {repository}")

    replace_once(root / "gradle.properties", f"group={OLD_GROUP}", f"group={group}")
    replace_once(
        root / "gradle.properties", f"pluginId={OLD_PLUGIN_ID}", f"pluginId={plugin_id}"
    )
    replace_once(
        root / "gradle.properties",
        f"pluginName={OLD_PLUGIN_NAME}",
        f"pluginName={plugin_name}",
    )
    replace_once(
        root / "gradle.properties",
        f"serverBinary={OLD_SERVER_BINARY}",
        f"serverBinary={server_binary}",
    )

    replace_once(
        root / "settings.gradle.kts",
        f'rootProject.name = "{OLD_ROOT_NAME}"',
        f'rootProject.name = "{repository}"',
    )

    manifest = root / "plugin/src/main/resources/META-INF/plugin.xml"
    replace_once(manifest, f"<id>{OLD_PLUGIN_ID}</id>", f"<id>{plugin_id}</id>")
    replace_once(manifest, f"<name>{OLD_PLUGIN_NAME}</name>", f"<name>{plugin_name}</name>")
    replace_once(
        manifest,
        f'<vendor url="https://github.com/{OLD_VENDOR}">{OLD_VENDOR}</vendor>',
        f'<vendor url="https://github.com/{owner}">{owner}</vendor>',
    )
    replace_once(
        manifest,
        f'implementation="{OLD_PLUGIN_ID}.LanguageServerIntegrationProvider"',
        f'implementation="{plugin_id}.LanguageServerIntegrationProvider"',
    )

    for source_root in JAVA_SOURCE_ROOTS:
        directory = root / source_root / OLD_PACKAGE_PATH
        for java in sorted(directory.glob("*.java")):
            replace_once(java, f"package {OLD_PLUGIN_ID};", f"package {plugin_id};")
        move_package(
            root / source_root,
            OLD_PACKAGE_PATH,
            package_path,
        )

    replace_once(
        root / "server/Cargo.toml",
        f'name = "{OLD_SERVER_BINARY}"',
        f'name = "{server_binary}"',
    )
    replace_once(
        root / "server/Cargo.toml",
        'description = "A minimal language server and IntelliJ Platform plugin built with lspf"',
        f'description = "The {plugin_name} language server"',
    )
    replace_all(
        root / "server/Cargo.lock",
        f'name = "{OLD_SERVER_BINARY}"',
        f'name = "{server_binary}"',
    )
    replace_once(
        root / "server/tests/protocol.rs",
        f'env!("CARGO_BIN_EXE_{OLD_SERVER_BINARY}")',
        f'env!("CARGO_BIN_EXE_{server_binary}")',
    )

    remove_identity_check(root)
    rewrite_readme(root, plugin_name, repository)

    shutil.rmtree(root / ".github/template-cleanup")
    (root / ".github/workflows/template-cleanup.yml").unlink()


if __name__ == "__main__":
    main()
