package io.github.meymchen.lspf.hello;

import com.intellij.platform.lsp.api.LspClientDescriptor;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class HelloLspIntegrationProviderTest extends BasePlatformTestCase {
    public void testHelloFileStartsClientWithBundledServer() throws Exception {
        var descriptor = new AtomicReference<LspClientDescriptor>();
        var file = new LightVirtualFile("example.hello", "hello");
        new HelloLspIntegrationProvider().fileOpened(getProject(), file, descriptor::set);

        assertNotNull(descriptor.get());
        assertTrue(descriptor.get().isSupportedFile(file));
        assertEquals("hello", descriptor.get().getLanguageId(file));
        var command = descriptor.get().createCommandLine();
        assertTrue(Files.isExecutable(Path.of(command.getExePath())));
        assertEquals("server", Path.of(command.getExePath()).getParent().getFileName().toString());
        assertTrue(command.getParametersList().getList().isEmpty());
    }

    public void testUnrelatedFileDoesNotStartClient() {
        var descriptor = new AtomicReference<LspClientDescriptor>();
        var file = new LightVirtualFile("example.txt", "hello");
        new HelloLspIntegrationProvider().fileOpened(getProject(), file, descriptor::set);
        assertNull(descriptor.get());
        assertFalse(new HelloLspClientDescriptor(getProject()).isSupportedFile(file));
    }
}
