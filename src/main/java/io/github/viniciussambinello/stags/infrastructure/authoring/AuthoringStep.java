package io.github.viniciussambinello.stags.infrastructure.authoring;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;

public sealed interface AuthoringStep {

    record CreateAwaitingIdentifier(CosmeticKind kind) implements AuthoringStep {
    }

    record CreateAwaitingPrefix(CosmeticKind kind, CosmeticId id) implements AuthoringStep {
    }

    record CreateAwaitingPermission(CosmeticKind kind, CosmeticId id, Prefix prefix) implements AuthoringStep {
    }

    record CreateAwaitingWeight(CosmeticKind kind, CosmeticId id, Prefix prefix, PermissionNode permission) implements AuthoringStep {
    }

    record CreateAwaitingConfirmation(Cosmetic candidate) implements AuthoringStep {
    }

    record EditAwaitingPrefix(Cosmetic existing) implements AuthoringStep {
    }

    record EditAwaitingPermission(Cosmetic existing, Prefix newPrefix) implements AuthoringStep {
    }

    record EditAwaitingWeight(Cosmetic existing, Prefix newPrefix, PermissionNode newPermission) implements AuthoringStep {
    }

    record EditAwaitingConfirmation(Cosmetic existing, Cosmetic candidate) implements AuthoringStep {
    }

    record DeleteAwaitingConfirmation(CosmeticKind kind, CosmeticId id) implements AuthoringStep {
    }
}
