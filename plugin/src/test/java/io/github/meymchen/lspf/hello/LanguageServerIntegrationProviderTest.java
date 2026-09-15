package io.github.meymchen.lspf.hello;

import com.intellij.platform.lsp.api.LspClientDescriptor;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class LanguageServerIntegrationProviderTest extends BasePlatformTestCase {
    /** The first configured extension, so the test holds for any valid configuration. */
    private static String extension() {
        return ServerMetadata.get("fileExtension").split(",")[0].trim();
    }

    private LspClientDescriptor clientStartedFor(String fileName) {
        var descriptor = new AtomicReference<LspClientDescriptor>();
        var file = new LightVirtualFile(fileName, "hello");
        new LanguageServerIntegrationProvider().fileOpened(getProject(), file, descriptor::set);
        return descriptor.get();
    }

    public void testConfiguredFileStartsClientWithBundledServer() throws Exception {
        var file = new LightVirtualFile("example." + extension(), "hello");
        var descriptor = new AtomicReference<LspClientDescriptor>();
        new LanguageServerIntegrationProvider().fileOpened(getProject(), file, descriptor::set);

        assertNotNull(descriptor.get());
        assertTrue(descriptor.get().isSupportedFile(file));
        assertEquals(ServerMetadata.get("languageId"), descriptor.get().getLanguageId(file));
        var command = descriptor.get().createCommandLine();
        assertTrue(Files.isExecutable(Path.of(command.getExePath())));
        assertEquals("server", Path.of(command.getExePath()).getParent().getFileName().toString());
        assertTrue(command.getParametersList().getList().isEmpty());
    }

    public void testFileMatchingFollowsTheConfiguredExtensions() {
        assertNotNull(clientStartedFor("example." + extension().toUpperCase(Locale.ROOT)));
        assertTrue(LanguageServerDescriptor.matchesExtension("hello, hi", "hi"));
        assertFalse(LanguageServerDescriptor.matchesExtension("hello, hi", "hello2"));
        // A file without an extension, such as a Makefile, must never match.
        assertFalse(LanguageServerDescriptor.matchesExtension("hello,", null));
        assertFalse(LanguageServerDescriptor.matchesExtension("hello,", ""));
    }

    public void testServerPathOverrideSelectsTheConfiguredExecutable() throws Exception {
        var directory = Files.createTempDirectory("lspf-server-override");
        var executable = Files.createFile(directory.resolve("debug-server"));
        assertTrue(executable.toFile().setExecutable(true));
        var property = ServerMetadata.get("serverName") + ".server.path";
        System.setProperty(property, executable.toString());
        try {
            var command = new LanguageServerDescriptor(getProject()).createCommandLine();
            assertEquals(executable.toString(), command.getExePath());
        } finally {
            System.clearProperty(property);
            Files.delete(executable);
            Files.delete(directory);
        }
    }

    public void testLogOverrideReachesTheServerAsRustLog() {
        var property = ServerMetadata.get("serverName") + ".server.log";
        System.setProperty(property, "debug");
        try {
            var command = new LanguageServerDescriptor(getProject()).createCommandLine();
            assertEquals("debug", command.getEnvironment().get("RUST_LOG"));
        } finally {
            System.clearProperty(property);
        }
    }

    public void testUnrelatedFileDoesNotStartClient() {
        var file = new LightVirtualFile("example.txt", "hello");
        assertNull(clientStartedFor("example.txt"));
        assertFalse(new LanguageServerDescriptor(getProject()).isSupportedFile(file));
    }
}
