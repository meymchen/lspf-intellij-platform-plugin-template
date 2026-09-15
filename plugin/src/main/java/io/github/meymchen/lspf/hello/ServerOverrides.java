package io.github.meymchen.lspf.hello;

import java.util.Locale;

/**
 * Resolves what the plugin launches, and how much the server logs.
 *
 * <p>Three sources answer each question, in order. The {@link ServerSettings settings page}
 * comes first, because it is the one a user can see. A system property of the running IDE
 * comes next, then an environment variable; both are named after the bundled server, so two
 * plugins built from this template never read each other's: a server named {@code my-language}
 * uses {@code my-language.server.path} and {@code MY_LANGUAGE_SERVER_PATH}.
 *
 * <p>All three belong to the IDE, not to the code it has open, so workspace contents can
 * never select the executable that the plugin runs.
 */
final class ServerOverrides {
    private ServerOverrides() {}

    /**
     * Path of a language server to launch instead of the bundled one, or {@code null}.
     *
     * <p>Relative paths resolve against the IDE's working directory, which is rarely what
     * you want; prefer an absolute path.
     */
    static String path() {
        return resolve("path", ServerSettings.serverPath());
    }

    /**
     * Filter for the server's own logging, or {@code null} to leave it to the environment.
     *
     * <p>The value reaches the server as {@code RUST_LOG}; see {@code server/src/log_format.rs}.
     */
    static String logFilter() {
        return resolve("log", ServerSettings.logFilter());
    }

    private static String resolve(String suffix, String configured) {
        if (configured != null) {
            return configured;
        }
        String server = ServerMetadata.get("serverName");
        String value = System.getProperty(server + ".server." + suffix);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable(server, suffix));
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String environmentVariable(String server, String suffix) {
        return (server + "_server_" + suffix).replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
    }
}
