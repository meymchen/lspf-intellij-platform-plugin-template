package io.github.meymchen.lspf.hello;

import java.util.Locale;

/**
 * Development-time overrides for the launched language server.
 *
 * <p>Each override is read from a system property of the running IDE, then from an
 * environment variable. Both are named after the bundled server, so two plugins built
 * from this template never read each other's settings: a server named {@code my-language}
 * uses {@code my-language.server.path} and {@code MY_LANGUAGE_SERVER_PATH}.
 *
 * <p>Both sources belong to the IDE process. An opened project cannot set either one, so
 * workspace contents can never select the executable that the plugin runs.
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
        return lookup("path");
    }

    /**
     * Filter for the server's own logging, or {@code null} to leave it to the environment.
     *
     * <p>The value reaches the server as {@code RUST_LOG}; see {@code server/src/log_format.rs}.
     */
    static String logFilter() {
        return lookup("log");
    }

    private static String lookup(String suffix) {
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
