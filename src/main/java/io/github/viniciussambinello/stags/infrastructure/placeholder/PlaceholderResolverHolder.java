package io.github.viniciussambinello.stags.infrastructure.placeholder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.github.viniciussambinello.stags.application.port.PlaceholderResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import net.kyori.adventure.text.Component;

public final class PlaceholderResolverHolder implements PlaceholderResolver {

    private final AtomicReference<PlaceholderResolver> active;

    public PlaceholderResolverHolder() {
        this.active = new AtomicReference<>(new NoopPlaceholderResolver());
    }

    public void activate(final PlaceholderResolver resolver) {
        active.set(resolver);
    }

    public void deactivate() {
        active.set(new NoopPlaceholderResolver());
    }

    @Override
    public Component resolve(final Prefix prefix, final UUID wearingPlayerId) {
        return active.get().resolve(prefix, wearingPlayerId);
    }
}
