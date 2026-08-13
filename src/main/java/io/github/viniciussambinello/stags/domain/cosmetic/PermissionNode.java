package io.github.viniciussambinello.stags.domain.cosmetic;

import java.util.Objects;

public record PermissionNode(String value) {

    public static final PermissionNode NONE = new PermissionNode("");

    public PermissionNode {
        Objects.requireNonNull(value, "value");
        value = value.trim();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }
}
