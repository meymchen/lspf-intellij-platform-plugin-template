package io.github.meymchen.lspf.hello;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * Connects the IDE to one language server per project.
 *
 * <p>Which files it claims, and the language ID it reports for them, come from
 * {@code fileExtension} and {@code languageId} in {@code gradle.properties}.
 */
final class LanguageServerDescriptor extends ProjectWideLspClientDescriptor {
    LanguageServerDescriptor(Project project) {
        super(project, ServerMetadata.get("pluginName"));
    }

    static boolean supports(VirtualFile file) {
        return !file.isDirectory()
                && matchesExtension(ServerMetadata.get("fileExtension"), file.getExtension());
    }

    /**
     * Matches a file extension against the configured list, which holds one extension
     * or several separated by commas.
     */
    static boolean matchesExtension(String configured, String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        for (String candidate : configured.split(",")) {
            if (candidate.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSupportedFile(@NotNull VirtualFile file) {
        return supports(file);
    }

    @Override
    public @NotNull String getLanguageId(@NotNull VirtualFile file) {
        return ServerMetadata.get("languageId");
    }

    @Override
    public @NotNull GeneralCommandLine createCommandLine() {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(ServerMetadata.get("pluginId")));
        if (plugin == null) {
            throw new IllegalStateException("Cannot locate the plugin installation");
        }
        var executable = ServerBinary.resolve(plugin.getPluginPath(), ServerMetadata.get("binary"));
        // Execute the bundled binary directly; workspace contents never select a command.
        return new GeneralCommandLine(executable.toString()).withCharset(StandardCharsets.UTF_8);
    }
}
