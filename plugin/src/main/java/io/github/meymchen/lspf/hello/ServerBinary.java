package io.github.meymchen.lspf.hello;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Locates the executable that the plugin launches as the language server. */
final class ServerBinary {
    private ServerBinary() {}

    /** Resolves the server packaged with the installed plugin. */
    static Path resolveBundled(Path pluginPath, String binary) {
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

    /**
     * Resolves a server named by a {@link ServerOverrides developer override}.
     *
     * <p>The path is taken as given: it names a build of the server that the developer
     * controls, so it is deliberately not restricted to the plugin's own directory.
     */
    static Path resolveOverride(String path) {
        Path executable;
        try {
            executable = Path.of(path).toAbsolutePath();
        } catch (InvalidPathException error) {
            throw new IllegalStateException("The configured language server path is not a valid path: " + path, error);
        }
        if (!Files.isRegularFile(executable) || !Files.isExecutable(executable)) {
            throw new IllegalStateException("The configured language server is missing or not executable: " + executable
                    + ". Check the server path override, or remove it to use the bundled server.");
        }
        return executable;
    }
}
