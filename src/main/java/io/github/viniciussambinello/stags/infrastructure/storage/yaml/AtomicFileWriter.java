package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class AtomicFileWriter {

    static void write(final Path target, final String content) {
        try {
            final Path parent = target.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            final Path tempFile = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException exception) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private AtomicFileWriter() {
    }
}
