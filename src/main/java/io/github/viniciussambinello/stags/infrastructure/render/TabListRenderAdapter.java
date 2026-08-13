package io.github.viniciussambinello.stags.infrastructure.render;

import java.util.Optional;

import org.bukkit.entity.Player;

import io.github.viniciussambinello.stags.application.port.PlaceholderResolver;
import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.infrastructure.config.TabListOrdering;
import net.kyori.adventure.text.Component;

public final class TabListRenderAdapter implements TargetRenderer {

    private final ConfigService configService;
    private final ActiveCosmeticResolver activeCosmeticResolver;
    private final PlaceholderResolver placeholderResolver;

    public TabListRenderAdapter(
            final ConfigService configService,
            final ActiveCosmeticResolver activeCosmeticResolver,
            final PlaceholderResolver placeholderResolver) {
        this.configService = configService;
        this.activeCosmeticResolver = activeCosmeticResolver;
        this.placeholderResolver = placeholderResolver;
    }

    @Override
    public void refresh(final Player player) {
        if (!configService.config().render().tabList().enabled()) {
            teardown(player);
            return;
        }
        final Optional<Cosmetic> activeTag = activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TAG);
        final Component prefix = activeTag
                .map(tag -> placeholderResolver.resolve(tag.prefix(), player.getUniqueId()).append(Component.space()))
                .orElse(Component.empty());
        player.playerListName(prefix.append(Component.text(player.getName())));

        if (configService.config().render().tabList().ordering() == TabListOrdering.WEIGHT) {
            final int weight = activeTag.map(tag -> tag.weight().value()).orElse(Integer.MIN_VALUE);
            player.setPlayerListOrder(weight);
        } else {
            player.setPlayerListOrder(0);
        }
    }

    @Override
    public void teardown(final Player player) {
        player.playerListName(null);
        player.setPlayerListOrder(0);
    }

    @Override
    public void shutdown() {
    }
}
