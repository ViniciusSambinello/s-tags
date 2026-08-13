package io.github.viniciussambinello.stags.domain.catalogue;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;

final class CatalogueRulesTest {

    private final CatalogueRules rules = new CatalogueRules();

    @Test
    void acceptsWellFormedNewCosmetic() {
        final ValidationResult result = rules.validateNew(
                CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100, Catalogue.empty());
        assertInstanceOf(ValidationResult.Accepted.class, result);
    }

    @Test
    void rejectsDuplicateIdentifierWithinSameKind() {
        final Catalogue catalogue = Catalogue.of(List.of(new Cosmetic(
                CosmeticKind.TAG,
                new CosmeticId("vip"),
                Prefix.parse("<gold>[VIP]</gold>"),
                PermissionNode.NONE,
                new Weight(100))));

        final ValidationResult result = rules.validateNew(
                CosmeticKind.TAG, "VIP", "<gold>[VIP2]</gold>", "stags.tag.vip", 50, catalogue);

        assertInstanceOf(ValidationResult.Rejected.class, result);
        assertInstanceOf(ValidationError.DuplicateIdentifier.class, ((ValidationResult.Rejected) result).error());
    }

    @Test
    void sameIdentifierIsAllowedAcrossDifferentKinds() {
        final Catalogue catalogue = Catalogue.of(List.of(new Cosmetic(
                CosmeticKind.TAG,
                new CosmeticId("vip"),
                Prefix.parse("<gold>[VIP]</gold>"),
                PermissionNode.NONE,
                new Weight(100))));

        final ValidationResult result = rules.validateNew(
                CosmeticKind.TITLE, "vip", "<gold>Champion</gold>", "stags.title.vip", 100, catalogue);

        assertInstanceOf(ValidationResult.Accepted.class, result);
    }

    @Test
    void rejectsMalformedIdentifier() {
        final ValidationResult result = rules.validateNew(
                CosmeticKind.TAG, "vip gold", "<gold>[VIP]</gold>", "stags.tag.vip", 100, Catalogue.empty());
        assertInstanceOf(ValidationResult.Rejected.class, result);
        assertInstanceOf(ValidationError.MalformedIdentifier.class, ((ValidationResult.Rejected) result).error());
    }

    @Test
    void rejectsMalformedPrefix() {
        final ValidationResult result = rules.validateNew(
                CosmeticKind.TAG, "vip", "<gold>[VIP]", "stags.tag.vip", 100, Catalogue.empty());
        assertInstanceOf(ValidationResult.Rejected.class, result);
        assertInstanceOf(ValidationError.MalformedPrefix.class, ((ValidationResult.Rejected) result).error());
    }

    @Test
    void rejectsNegativeWeight() {
        final ValidationResult result = rules.validateNew(
                CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", -1, Catalogue.empty());
        assertInstanceOf(ValidationResult.Rejected.class, result);
        assertInstanceOf(ValidationError.InvalidWeight.class, ((ValidationResult.Rejected) result).error());
    }
}
