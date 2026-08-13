package io.github.viniciussambinello.stags.infrastructure.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

final class StorageExecutorTest {

    @Test
    void submittedWorkRunsOffTheCallingThread() throws Exception {
        try (StorageExecutor executor = new StorageExecutor()) {
            final String callingThread = Thread.currentThread().getName();
            final String executionThread = executor.submit(() -> Thread.currentThread().getName()).get();
            assertNotEquals(callingThread, executionThread);
            assertEquals("s-tags-storage", executionThread);
        }
    }

    @Test
    void workIsSerializedOnASingleThread() throws Exception {
        try (StorageExecutor executor = new StorageExecutor()) {
            final String first = executor.submit(() -> Thread.currentThread().getName()).get();
            final String second = executor.submit(() -> Thread.currentThread().getName()).get();
            assertEquals(first, second);
        }
    }
}
