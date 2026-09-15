package io.github.meymchen.lspf.hello;

import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.concurrent.atomic.AtomicReference;

public class ServerSettingsTest extends BasePlatformTestCase {
    @Override
    protected void tearDown() throws Exception {
        try {
            // The settings outlive a single test, so every test has to leave them unset.
            ServerSettings.setEnabled(true);
            ServerSettings.setServerPath(null);
            ServerSettings.setLogFilter(null);
        } finally {
            super.tearDown();
        }
    }

    public void testDefaultsAreUnsetAndEnabled() {
        assertTrue(ServerSettings.isEnabled());
        assertNull(ServerSettings.serverPath());
        assertNull(ServerSettings.logFilter());
    }

    public void testBlankValuesReadBackAsUnset() {
        ServerSettings.setServerPath("   ");
        ServerSettings.setLogFilter("");
        assertNull(ServerSettings.serverPath());
        assertNull(ServerSettings.logFilter());
    }

    public void testSettingsWinOverTheLaunchOverride() {
        var property = ServerMetadata.get("serverName") + ".server.log";
        System.setProperty(property, "from-property");
        try {
            assertEquals("from-property", ServerOverrides.logFilter());
            ServerSettings.setLogFilter(" from-settings ");
            assertEquals("from-settings", ServerOverrides.logFilter());
            ServerSettings.setLogFilter(null);
            assertEquals("from-property", ServerOverrides.logFilter());
        } finally {
            System.clearProperty(property);
        }
    }

    public void testDisablingStopsTheClientFromStarting() {
        var extension = ServerMetadata.get("fileExtension").split(",")[0].trim();
        var file = new LightVirtualFile("example." + extension, "hello");
        var descriptor = new AtomicReference<>();
        ServerSettings.setEnabled(false);
        new LanguageServerIntegrationProvider().fileOpened(getProject(), file, descriptor::set);
        assertNull(descriptor.get());

        ServerSettings.setEnabled(true);
        new LanguageServerIntegrationProvider().fileOpened(getProject(), file, descriptor::set);
        assertNotNull(descriptor.get());
    }

    public void testConfigurablePresentsAndStoresTheSettings() {
        var configurable = new ServerConfigurable();
        assertEquals(ServerMetadata.get("pluginName"), configurable.getDisplayName());
        assertNotNull(configurable.createComponent());

        configurable.reset();
        assertFalse(configurable.isModified());

        ServerSettings.setLogFilter("trace");
        ServerSettings.setEnabled(false);
        assertTrue(configurable.isModified());
        configurable.reset();
        assertFalse(configurable.isModified());

        // Apply writes the fields back. Applying while disabled only stops the servers; the
        // enabled path also asks the platform to start them, which needs this plugin's
        // extension point and therefore only happens in a running IDE.
        ServerSettings.setLogFilter(null);
        configurable.apply();
        assertEquals("trace", ServerSettings.logFilter());
        assertFalse(ServerSettings.isEnabled());
    }
}
