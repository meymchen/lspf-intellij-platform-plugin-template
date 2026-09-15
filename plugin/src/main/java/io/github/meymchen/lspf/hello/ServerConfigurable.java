package io.github.meymchen.lspf.hello;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.platform.lsp.api.LspClientManager;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * The plugin's settings page, registered in {@code plugin.xml} under Settings | Tools.
 *
 * <p>The status bar widget links here, and applying a change restarts the running servers.
 */
final class ServerConfigurable implements Configurable {
    private final JBCheckBox enabled = new JBCheckBox("Start the language server for supported files");
    private final TextFieldWithBrowseButton serverPath = new TextFieldWithBrowseButton();
    private final JBTextField logFilter = new JBTextField();

    @Override
    public @NotNull String getDisplayName() {
        // The registration is dynamic, so the page is named from gradle.properties.
        return ServerMetadata.get("pluginName");
    }

    @Override
    public JComponent createComponent() {
        serverPath.addBrowseFolderListener(null, FileChooserDescriptorFactory.singleFile());
        return FormBuilder.createFormBuilder()
                .addComponent(enabled)
                .addLabeledComponent("Server executable:", serverPath)
                .addComponentToRightColumn(new JBLabel("Empty launches the server bundled with the plugin."))
                .addLabeledComponent("Log filter:", logFilter)
                .addComponentToRightColumn(new JBLabel("Reaches the server as RUST_LOG, for example debug."))
                .getPanel();
    }

    @Override
    public boolean isModified() {
        return enabled.isSelected() != ServerSettings.isEnabled()
                || !text(serverPath.getText()).equals(text(ServerSettings.serverPath()))
                || !text(logFilter.getText()).equals(text(ServerSettings.logFilter()));
    }

    @Override
    public void apply() {
        ServerSettings.setEnabled(enabled.isSelected());
        ServerSettings.setServerPath(serverPath.getText());
        ServerSettings.setLogFilter(logFilter.getText());
        applyToRunningClients();
    }

    @Override
    public void reset() {
        enabled.setSelected(ServerSettings.isEnabled());
        serverPath.setText(text(ServerSettings.serverPath()));
        logFilter.setText(text(ServerSettings.logFilter()));
    }

    /** Brings the servers of every open project in line with the settings just applied. */
    static void applyToRunningClients() {
        boolean start = ServerSettings.isEnabled();
        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            var clients = LspClientManager.getInstance(project);
            if (start) {
                clients.stopAndRestartClientsIfNeeded(LanguageServerIntegrationProvider.class);
            } else {
                clients.stopClients(LanguageServerIntegrationProvider.class);
            }
        }
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
