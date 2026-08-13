package io.github.viniciussambinello.stags.infrastructure.placeholder;

import java.util.UUID;

import io.github.viniciussambinello.stags.application.port.PlaceholderResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import net.kyori.adventure.text.Component;

public final class NoopPlaceholderResolver implements PlaceholderResolver {

    @Override
    public Component resolve(final Prefix prefix, final UUID wearingPlayerId) {
        return prefix.rendered();
    }
}
