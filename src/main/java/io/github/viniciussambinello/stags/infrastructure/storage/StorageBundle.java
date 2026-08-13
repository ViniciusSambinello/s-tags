package io.github.viniciussambinello.stags.infrastructure.storage;

import io.github.viniciussambinello.stags.application.port.CosmeticRepository;
import io.github.viniciussambinello.stags.application.port.SelectionRepository;
import io.github.viniciussambinello.stags.infrastructure.config.StorageBackend;

public record StorageBundle(
        StorageBackend activeBackend,
        CosmeticRepository cosmeticRepository,
        SelectionRepository selectionRepository,
        AutoCloseable closeable) {
}
