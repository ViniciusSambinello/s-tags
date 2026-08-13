package io.github.viniciussambinello.stags.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectorCooldownService {

    private final Clock clock;
    private final Map<UUID, Instant> lastOpen;
    private final Map<UUID, Instant> lastSelect;

    public SelectorCooldownService(final Clock clock) {
        this.clock = clock;
        this.lastOpen = new ConcurrentHashMap<>();
        this.lastSelect = new ConcurrentHashMap<>();
    }

    public boolean tryOpen(final UUID playerId, final Duration cooldown) {
        return tryAdvance(lastOpen, playerId, cooldown);
    }

    public boolean trySelect(final UUID playerId, final Duration cooldown) {
        return tryAdvance(lastSelect, playerId, cooldown);
    }

    private boolean tryAdvance(final Map<UUID, Instant> tracker, final UUID playerId, final Duration cooldown) {
        final Instant now = clock.instant();
        final Instant previous = tracker.get(playerId);
        if (previous != null && Duration.between(previous, now).compareTo(cooldown) < 0) {
            return false;
        }
        tracker.put(playerId, now);
        return true;
    }
}
