package io.github.meymchen.lspf.hello;

import com.intellij.ide.util.PropertiesComponent;

/**
 * The settings shown in the plugin's own settings page, stored per IDE installation.
 *
 * <p>Every key carries this plugin's ID, so two plugins built from this template keep
 * separate values without either one naming the other.
 */
final class ServerSettings {
    private ServerSettings() {}

    /** Whether the plugin starts a language server at all. */
    static boolean isEnabled() {
        return PropertiesComponent.getInstance().getBoolean(key("enabled"), true);
    }

    static void setEnabled(boolean enabled) {
        PropertiesComponent.getInstance().setValue(key("enabled"), enabled, true);
    }

    /** A server to launch instead of the bundled one, or {@code null} to use the bundled one. */
    static String serverPath() {
        return value("path");
    }

    static void setServerPath(String path) {
        store("path", path);
    }

    /** The filter for the server's own logging, or {@code null} to leave it to the environment. */
    static String logFilter() {
        return value("log");
    }

    static void setLogFilter(String filter) {
        store("log", filter);
    }

    private static String key(String suffix) {
        return ServerMetadata.get("pluginId") + ".server." + suffix;
    }

    private static String value(String suffix) {
        String value = PropertiesComponent.getInstance().getValue(key(suffix));
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void store(String suffix, String value) {
        // An empty field means "unset", which is what falls back to the launch overrides.
        if (value == null || value.isBlank()) {
            PropertiesComponent.getInstance().unsetValue(key(suffix));
        } else {
            PropertiesComponent.getInstance().setValue(key(suffix), value.trim());
        }
    }
}
