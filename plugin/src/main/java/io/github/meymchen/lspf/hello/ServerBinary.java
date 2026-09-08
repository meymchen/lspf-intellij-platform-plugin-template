package io.github.meymchen.lspf.hello;

import java.nio.file.Files;
import java.nio.file.Path;

final class ServerBinary {
    private ServerBinary() {}

    static Path resolve(Path pluginPath, String binary) {
        if (binary == null || !binary.matches("[A-Za-z0-9_-]+(?:\\.exe)?")) {
            throw new IllegalStateException("Invalid bundled server name");
        }
        Path executable = pluginPath.resolve("server").resolve(binary);
        if (!Files.isRegularFile(executable) || !Files.isExecutable(executable)) {
            throw new IllegalStateException("Bundled language server is missing or not executable: " + executable
                    + ". Install the plugin package built for your operating system and architecture.");
        }
        return executable;
    }
}
