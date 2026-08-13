package io.github.viniciussambinello.stags.domain.catalogue;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.PrefixParseException;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;

public final class CatalogueRules {

    public ValidationResult validateNew(
            final CosmeticKind kind,
            final String rawId,
            final String rawPrefix,
            final String rawPermission,
            final int rawWeight,
            final Catalogue catalogue) {
        final CosmeticId id;
        try {
            id = new CosmeticId(rawId);
        } catch (final IllegalArgumentException exception) {
            return new ValidationResult.Rejected(new ValidationError.MalformedIdentifier(exception.getMessage()));
        }

        if (catalogue.contains(kind, id)) {
            return new ValidationResult.Rejected(new ValidationError.DuplicateIdentifier(id));
        }

        final Prefix prefix;
        try {
            prefix = Prefix.parse(rawPrefix);
        } catch (final PrefixParseException exception) {
            return new ValidationResult.Rejected(new ValidationError.MalformedPrefix(exception.getMessage()));
        }

        final PermissionNode permission = new PermissionNode(rawPermission);
        final Weight weight = new Weight(rawWeight);
        return new ValidationResult.Accepted(new Cosmetic(kind, id, prefix, permission, weight));
    }

    public ValidationResult validateEdit(
            final Cosmetic existing,
            final String rawPrefix,
            final String rawPermission,
            final int rawWeight) {
        final Prefix prefix;
        try {
            prefix = Prefix.parse(rawPrefix);
        } catch (final PrefixParseException exception) {
            return new ValidationResult.Rejected(new ValidationError.MalformedPrefix(exception.getMessage()));
        }

        final PermissionNode permission = new PermissionNode(rawPermission);
        final Weight weight = new Weight(rawWeight);
        return new ValidationResult.Accepted(existing.withPrefix(prefix).withPermission(permission).withWeight(weight));
    }
}
