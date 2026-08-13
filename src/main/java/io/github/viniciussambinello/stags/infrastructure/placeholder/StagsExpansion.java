package io.github.viniciussambinello.stags.infrastructure.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import io.github.viniciussambinello.stags.application.port.PermissionOracle;
import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.usecase.OwnershipCheck;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class StagsExpansion extends PlaceholderExpansion {

    private final String pluginVersion;
    private final ActiveCosmeticResolver activeCosmeticResolver;
    private final CatalogueService catalogueService;
    private final PermissionOracle permissionOracle;

    public StagsExpansion(
            final String pluginVersion,
            final ActiveCosmeticResolver activeCosmeticResolver,
            final CatalogueService catalogueService,
            final PermissionOracle permissionOracle) {
        this.pluginVersion = pluginVersion;
        this.activeCosmeticResolver = activeCosmeticResolver;
        this.catalogueService = catalogueService;
        this.permissionOracle = permissionOracle;
    }

    @Override
    public String getIdentifier() {
        return "stags";
    }

    @Override
    public String getAuthor() {
        return "ViniciusSambinello";
    }

    @Override
    public String getVersion() {
        return pluginVersion;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer offlinePlayer, final String params) {
        if (!(offlinePlayer instanceof Player player) || !player.isOnline()) {
            return isKnownPlaceholder(params) ? "" : null;
        }
        return switch (params) {
            case "tag" -> activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TAG)
                    .map(cosmetic -> cosmetic.id().value()).orElse("");
            case "tag_prefix" -> activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TAG)
                    .map(this::plainText).orElse("");
            case "tag_weight" -> activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TAG)
                    .map(cosmetic -> String.valueOf(cosmetic.weight().value())).orElse("");
            case "title" -> activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TITLE)
                    .map(cosmetic -> cosmetic.id().value()).orElse("");
            case "title_text" -> activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TITLE)
                    .map(this::plainText).orElse("");
            case "tag_count" -> String.valueOf(ownedCount(player, CosmeticKind.TAG));
            case "title_count" -> String.valueOf(ownedCount(player, CosmeticKind.TITLE));
            default -> null;
        };
    }

    private boolean isKnownPlaceholder(final String params) {
        return switch (params) {
            case "tag", "tag_prefix", "tag_weight", "title", "title_text", "tag_count", "title_count" -> true;
            default -> false;
        };
    }

    private String plainText(final Cosmetic cosmetic) {
        return PlainTextComponentSerializer.plainText().serialize(cosmetic.prefix().rendered());
    }

    private long ownedCount(final Player player, final CosmeticKind kind) {
        return catalogueService.catalogue().all(kind).stream()
                .filter(cosmetic -> OwnershipCheck.owns(permissionOracle, player.getUniqueId(), cosmetic))
                .count();
    }
}
