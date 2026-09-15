package io.github.meymchen.lspf.hello;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class ServerBinaryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Path executableFile(Path parent, String name) throws Exception {
        Files.createDirectories(parent);
        var executable = Files.createFile(parent.resolve(name));
        assertTrue(executable.toFile().setExecutable(true));
        return executable;
    }

    @Test public void findsBinaryUnderPluginPathWithSpaces() throws Exception {
        var plugin = temporary.newFolder("plugin with spaces").toPath();
        var executable = executableFile(plugin.resolve("server"), "example-server");
        assertEquals(executable, ServerBinary.resolveBundled(plugin, "example-server"));
    }

    @Test public void rejectsMissingBinary() {
        assertThrows(IllegalStateException.class,
                () -> ServerBinary.resolveBundled(temporary.getRoot().toPath(), "missing"));
    }

    @Test public void rejectsPathsOutsideBundle() {
        for (String name : new String[]{"../other", "C:\\other.exe", "/tmp/other", "server/other"}) {
            assertThrows(IllegalStateException.class,
                    () -> ServerBinary.resolveBundled(temporary.getRoot().toPath(), name));
        }
    }

    @Test public void overrideAcceptsAnExecutableOutsideTheBundle() throws Exception {
        var executable = executableFile(temporary.newFolder("elsewhere").toPath(), "debug-server");
        assertEquals(executable, ServerBinary.resolveOverride(executable.toString()));
    }

    @Test public void overrideRejectsMissingAndNonExecutableTargets() throws Exception {
        var directory = temporary.newFolder("not-a-server").toPath();
        assertThrows(IllegalStateException.class,
                () -> ServerBinary.resolveOverride(directory.resolve("absent").toString()));
        assertThrows(IllegalStateException.class, () -> ServerBinary.resolveOverride(directory.toString()));
    }
}
