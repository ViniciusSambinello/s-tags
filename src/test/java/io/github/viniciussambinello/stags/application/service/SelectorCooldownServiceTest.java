package io.github.viniciussambinello.stags.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

final class SelectorCooldownServiceTest {

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        MutableClock(final Instant start) {
            this.now = new AtomicReference<>(start);
        }

        void advance(final Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    @Test
    void secondAttemptWithinCooldownIsRejected() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final SelectorCooldownService service = new SelectorCooldownService(clock);
        final UUID playerId = UUID.randomUUID();

        assertTrue(service.tryOpen(playerId, Duration.ofSeconds(2)));
        assertFalse(service.tryOpen(playerId, Duration.ofSeconds(2)));
    }

    @Test
    void attemptAfterCooldownElapsesIsAccepted() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final SelectorCooldownService service = new SelectorCooldownService(clock);
        final UUID playerId = UUID.randomUUID();

        assertTrue(service.tryOpen(playerId, Duration.ofSeconds(2)));
        clock.advance(Duration.ofSeconds(3));
        assertTrue(service.tryOpen(playerId, Duration.ofSeconds(2)));
    }

    @Test
    void openAndSelectCooldownsAreIndependent() {
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final SelectorCooldownService service = new SelectorCooldownService(clock);
        final UUID playerId = UUID.randomUUID();

        assertTrue(service.tryOpen(playerId, Duration.ofSeconds(5)));
        assertTrue(service.trySelect(playerId, Duration.ofSeconds(5)));
    }
}
