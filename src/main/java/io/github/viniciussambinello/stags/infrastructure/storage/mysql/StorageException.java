package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

public final class StorageException extends RuntimeException {

    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
