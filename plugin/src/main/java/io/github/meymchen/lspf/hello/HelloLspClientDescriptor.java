package io.github.meymchen.lspf.hello;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

final class HelloLspClientDescriptor extends ProjectWideLspClientDescriptor {
    HelloLspClientDescriptor(Project project) {
        super(project, ServerMetadata.get("pluginName"));
    }

    static boolean supports(VirtualFile file) {
        return !file.isDirectory() && "hello".equals(file.getExtension());
    }

    @Override
    public boolean isSupportedFile(@NotNull VirtualFile file) {
        return supports(file);
    }

    @Override
    public @NotNull String getLanguageId(@NotNull VirtualFile file) {
        return "hello";
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
