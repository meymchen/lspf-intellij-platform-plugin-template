package io.github.meymchen.lspf.hello;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspClient;
import com.intellij.platform.lsp.api.LspIntegrationProvider;
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LanguageServerIntegrationProvider implements LspIntegrationProvider {
    @Override
    public void fileOpened(@NotNull Project project, @NotNull VirtualFile file,
                           @NotNull LspClientStarter clientStarter) {
        if (ServerSettings.isEnabled() && LanguageServerDescriptor.supports(file)) {
            clientStarter.ensureClientStarted(new LanguageServerDescriptor(project));
        }
    }

    @Override
    public @NotNull LspClientWidgetItem createWidgetItem(@NotNull LspClient lspClient, @Nullable VirtualFile currentFile) {
        // Gives this plugin its own entry in the Language Services widget, linked to its
        // settings page.
        // TODO(template): ship your own icon and use it here.
        return new LspClientWidgetItem(lspClient, currentFile, AllIcons.Webreferences.Server,
                ServerConfigurable.class);
    }
}
