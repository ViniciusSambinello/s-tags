package io.github.viniciussambinello.stags.domain.cosmetic;

import java.util.Objects;

public record Cosmetic(CosmeticKind kind, CosmeticId id, Prefix prefix, PermissionNode permission, Weight weight) {

    public Cosmetic {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(weight, "weight");
    }

    public Cosmetic withPrefix(final Prefix newPrefix) {
        return new Cosmetic(kind, id, newPrefix, permission, weight);
    }

    public Cosmetic withPermission(final PermissionNode newPermission) {
        return new Cosmetic(kind, id, prefix, newPermission, weight);
    }

    public Cosmetic withWeight(final Weight newWeight) {
        return new Cosmetic(kind, id, prefix, permission, newWeight);
    }
}
