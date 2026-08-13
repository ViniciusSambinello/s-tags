package io.github.viniciussambinello.stags.domain.catalogue;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;

public sealed interface ValidationResult {

    record Accepted(Cosmetic cosmetic) implements ValidationResult {
    }

    record Rejected(ValidationError error) implements ValidationResult {
    }
}
