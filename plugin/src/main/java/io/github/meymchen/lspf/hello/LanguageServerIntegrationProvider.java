package io.github.meymchen.lspf.hello;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspIntegrationProvider;
import org.jetbrains.annotations.NotNull;

public final class LanguageServerIntegrationProvider implements LspIntegrationProvider {
    @Override
    public void fileOpened(@NotNull Project project, @NotNull VirtualFile file,
                           @NotNull LspClientStarter clientStarter) {
        if (LanguageServerDescriptor.supports(file)) {
            clientStarter.ensureClientStarted(new LanguageServerDescriptor(project));
        }
    }
}
