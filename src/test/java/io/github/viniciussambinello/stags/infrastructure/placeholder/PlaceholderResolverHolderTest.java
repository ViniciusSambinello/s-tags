package io.github.viniciussambinello.stags.infrastructure.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.application.port.PlaceholderResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import net.kyori.adventure.text.Component;

final class PlaceholderResolverHolderTest {

    @Test
    void defaultsToNoopBehavior() {
        final PlaceholderResolverHolder holder = new PlaceholderResolverHolder();
        final Prefix prefix = Prefix.parse("<gold>%player_name%</gold>");
        assertEquals(prefix.rendered(), holder.resolve(prefix, UUID.randomUUID()));
    }

    @Test
    void delegatesToTheActivatedResolver() {
        final PlaceholderResolverHolder holder = new PlaceholderResolverHolder();
        final Component replacement = Component.text("resolved");
        final PlaceholderResolver stub = (prefix, playerId) -> replacement;
        holder.activate(stub);

        assertEquals(replacement, holder.resolve(Prefix.parse("<gold>x</gold>"), UUID.randomUUID()));
    }

    @Test
    void deactivateReturnsToNoopBehavior() {
        final PlaceholderResolverHolder holder = new PlaceholderResolverHolder();
        holder.activate((prefix, playerId) -> Component.text("resolved"));
        holder.deactivate();

        final Prefix prefix = Prefix.parse("<gold>[VIP]</gold>");
        assertEquals(prefix.rendered(), holder.resolve(prefix, UUID.randomUUID()));
    }
}
