package com.iimmersao.springmimic.server;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class SecureTempFileManagerFactory implements NanoHTTPD.TempFileManagerFactory {

    private static final FileAttribute<Set<PosixFilePermission>> PRIVATE_DIRECTORY_PERMISSIONS =
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
    private static final FileAttribute<Set<PosixFilePermission>> PRIVATE_FILE_PERMISSIONS =
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    @Override
    public NanoHTTPD.TempFileManager create() {
        return new SecureTempFileManager();
    }

    private static class SecureTempFileManager implements NanoHTTPD.TempFileManager {

        private final Path directory;
        private final List<Path> tempFiles = new ArrayList<>();

        private SecureTempFileManager() {
            this.directory = createPrivateTempDirectory();
        }

        @Override
        public void clear() {
            for (Path tempFile : tempFiles) {
                deleteIfExists(tempFile);
            }
            tempFiles.clear();
            deleteIfExists(directory);
        }

        @Override
        public NanoHTTPD.TempFile createTempFile(String filenameHint) throws IOException {
            String safePrefix = sanitizePrefix(filenameHint);
            Path tempFile = createPrivateTempFile(directory, safePrefix, ".tmp");
            tempFiles.add(tempFile);
            return new SecureTempFile(tempFile);
        }

        private static Path createPrivateTempDirectory() {
            try {
                if (supportsPosixPermissions()) {
                    return Files.createTempDirectory("springmimic-nanohttpd-", PRIVATE_DIRECTORY_PERMISSIONS);
                }
                return Files.createTempDirectory("springmimic-nanohttpd-");
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create NanoHTTPD temp directory", e);
            }
        }

        private static Path createPrivateTempFile(Path directory, String prefix, String suffix) throws IOException {
            if (supportsPosixPermissions()) {
                return Files.createTempFile(directory, prefix, suffix, PRIVATE_FILE_PERMISSIONS);
            }
            return Files.createTempFile(directory, prefix, suffix);
        }

        private static boolean supportsPosixPermissions() {
            return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        }

        private static String sanitizePrefix(String filenameHint) {
            String sanitized = filenameHint == null ? "body" : filenameHint.replaceAll("[^A-Za-z0-9._-]", "_");
            if (sanitized.length() < 3) {
                return (sanitized + "___").substring(0, 3);
            }
            return sanitized;
        }

        private static void deleteIfExists(Path path) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Temp files are best-effort cleanup; NanoHTTPD offers no recovery hook here.
            }
        }
    }

    private static class SecureTempFile implements NanoHTTPD.TempFile {

        private final Path path;

        private SecureTempFile(Path path) {
            this.path = path;
        }

        @Override
        public void delete() throws IOException {
            Files.deleteIfExists(path);
        }

        @Override
        public String getName() {
            return path.toString();
        }

        @Override
        public OutputStream open() throws IOException {
            return Files.newOutputStream(path);
        }
    }
}
