package com.iimmersao.springmimic.server;

import fi.iki.elonen.NanoHTTPD;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureTempFileManagerFactoryTest {

    @Test
    void shouldCreateAndCleanPrivateTempFiles() throws Exception {
        NanoHTTPD.TempFileManager manager = new SecureTempFileManagerFactory().create();
        NanoHTTPD.TempFile tempFile = manager.createTempFile("../body");
        Path tempPath = Path.of(tempFile.getName());
        Path tempDirectory = tempPath.getParent();

        try (OutputStream output = tempFile.open()) {
            output.write("request body".getBytes(StandardCharsets.UTF_8));
        }

        assertTrue(Files.exists(tempDirectory));
        assertTrue(Files.exists(tempPath));

        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            assertEquals(Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ), Files.getPosixFilePermissions(tempDirectory));
            assertEquals(Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ), Files.getPosixFilePermissions(tempPath));
        }

        manager.clear();

        assertFalse(Files.exists(tempPath));
        assertFalse(Files.exists(tempDirectory));
    }
}
