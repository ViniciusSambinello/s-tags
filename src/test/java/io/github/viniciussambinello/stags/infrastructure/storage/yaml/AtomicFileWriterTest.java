package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AtomicFileWriterTest {

    @Test
    void writeProducesReadableContent(@TempDir final Path dir) throws Exception {
        final Path target = dir.resolve("data.yml");
        AtomicFileWriter.write(target, "key: value\n");
        assertEquals("key: value\n", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void successfulWriteLeavesNoTempFileBehind(@TempDir final Path dir) throws Exception {
        final Path target = dir.resolve("data.yml");
        AtomicFileWriter.write(target, "key: value\n");
        assertEquals(0, countTempFiles(dir));
    }

    @Test
    void anOrphanedIncompleteTempFileNeverCorruptsThePreviousTarget(@TempDir final Path dir) throws Exception {
        final Path target = dir.resolve("data.yml");
        AtomicFileWriter.write(target, "key: original\n");

        final Path orphanedPartialWrite = Files.createTempFile(dir, "data.yml", ".tmp");
        Files.writeString(orphanedPartialWrite, "key: gar", StandardCharsets.UTF_8);

        assertEquals("key: original\n", Files.readString(target, StandardCharsets.UTF_8));
        assertTrue(YamlFiles.loadOrEmpty(target).isString("key"));
    }

    @Test
    void aLaterSuccessfulWriteStillWorksAlongsideAnOrphanedTempFile(@TempDir final Path dir) throws Exception {
        final Path target = dir.resolve("data.yml");
        AtomicFileWriter.write(target, "key: original\n");
        Files.writeString(Files.createTempFile(dir, "data.yml", ".tmp"), "garbage", StandardCharsets.UTF_8);

        AtomicFileWriter.write(target, "key: updated\n");

        assertEquals("key: updated\n", Files.readString(target, StandardCharsets.UTF_8));
    }

    private long countTempFiles(final Path dir) throws Exception {
        try (var stream = Files.list(dir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".tmp")).count();
        }
    }
}
