package io.github.viniciussambinello.stags.domain.catalogue;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;

public sealed interface ValidationError {

    record DuplicateIdentifier(CosmeticId id) implements ValidationError {
    }

    record MalformedIdentifier(String reason) implements ValidationError {
    }

    record MalformedPrefix(String reason) implements ValidationError {
    }
}
