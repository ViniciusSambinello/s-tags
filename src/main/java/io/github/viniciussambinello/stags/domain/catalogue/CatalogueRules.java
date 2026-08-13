package io.github.viniciussambinello.stags.domain.catalogue;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.PrefixParseException;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;

public final class CatalogueRules {

    public FieldValidation<CosmeticId> validateIdentifier(final String rawId, final CosmeticKind kind, final Catalogue catalogue) {
        final CosmeticId id;
        try {
            id = new CosmeticId(rawId);
        } catch (final IllegalArgumentException exception) {
            return new FieldValidation.Invalid<>(new ValidationError.MalformedIdentifier(exception.getMessage()));
        }
        if (catalogue.contains(kind, id)) {
            return new FieldValidation.Invalid<>(new ValidationError.DuplicateIdentifier(id));
        }
        return new FieldValidation.Valid<>(id);
    }

    public FieldValidation<Prefix> validatePrefix(final String rawPrefix) {
        try {
            return new FieldValidation.Valid<>(Prefix.parse(rawPrefix));
        } catch (final PrefixParseException exception) {
            return new FieldValidation.Invalid<>(new ValidationError.MalformedPrefix(exception.getMessage()));
        }
    }

    public FieldValidation<Weight> validateWeight(final int rawWeight) {
        try {
            return new FieldValidation.Valid<>(new Weight(rawWeight));
        } catch (final IllegalArgumentException exception) {
            return new FieldValidation.Invalid<>(new ValidationError.InvalidWeight(exception.getMessage()));
        }
    }

    public ValidationResult validateNew(
            final CosmeticKind kind,
            final String rawId,
            final String rawPrefix,
            final String rawPermission,
            final int rawWeight,
            final Catalogue catalogue) {
        final FieldValidation<CosmeticId> idValidation = validateIdentifier(rawId, kind, catalogue);
        if (idValidation instanceof FieldValidation.Invalid<CosmeticId> invalid) {
            return new ValidationResult.Rejected(invalid.error());
        }
        final CosmeticId id = ((FieldValidation.Valid<CosmeticId>) idValidation).value();

        final FieldValidation<Prefix> prefixValidation = validatePrefix(rawPrefix);
        if (prefixValidation instanceof FieldValidation.Invalid<Prefix> invalid) {
            return new ValidationResult.Rejected(invalid.error());
        }
        final Prefix prefix = ((FieldValidation.Valid<Prefix>) prefixValidation).value();

        final FieldValidation<Weight> weightValidation = validateWeight(rawWeight);
        if (weightValidation instanceof FieldValidation.Invalid<Weight> invalid) {
            return new ValidationResult.Rejected(invalid.error());
        }
        final Weight weight = ((FieldValidation.Valid<Weight>) weightValidation).value();

        final PermissionNode permission = new PermissionNode(rawPermission);
        return new ValidationResult.Accepted(new Cosmetic(kind, id, prefix, permission, weight));
    }

    public ValidationResult validateEdit(
            final Cosmetic existing,
            final String rawPrefix,
            final String rawPermission,
            final int rawWeight) {
        final FieldValidation<Prefix> prefixValidation = validatePrefix(rawPrefix);
        if (prefixValidation instanceof FieldValidation.Invalid<Prefix> invalid) {
            return new ValidationResult.Rejected(invalid.error());
        }
        final Prefix prefix = ((FieldValidation.Valid<Prefix>) prefixValidation).value();

        final FieldValidation<Weight> weightValidation = validateWeight(rawWeight);
        if (weightValidation instanceof FieldValidation.Invalid<Weight> invalid) {
            return new ValidationResult.Rejected(invalid.error());
        }
        final Weight weight = ((FieldValidation.Valid<Weight>) weightValidation).value();

        final PermissionNode permission = new PermissionNode(rawPermission);
        return new ValidationResult.Accepted(existing.withPrefix(prefix).withPermission(permission).withWeight(weight));
    }
}
