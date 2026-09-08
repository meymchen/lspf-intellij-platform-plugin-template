package io.github.meymchen.lspf.hello;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ServerBinaryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void findsBinaryUnderPluginPathWithSpaces() throws Exception {
        var plugin = temporary.newFolder("plugin with spaces").toPath();
        var server = Files.createDirectories(plugin.resolve("server"));
        var executable = Files.createFile(server.resolve("example-server"));
        assertTrue(executable.toFile().setExecutable(true));
        assertEquals(executable, ServerBinary.resolve(plugin, "example-server"));
    }

    @Test public void rejectsMissingBinary() {
        assertThrows(IllegalStateException.class,
                () -> ServerBinary.resolve(temporary.getRoot().toPath(), "missing"));
    }

    @Test public void rejectsPathsOutsideBundle() {
        for (String name : new String[]{"../other", "C:\\other.exe", "/tmp/other", "server/other"}) {
            assertThrows(IllegalStateException.class,
                    () -> ServerBinary.resolve(temporary.getRoot().toPath(), name));
        }
    }
}
