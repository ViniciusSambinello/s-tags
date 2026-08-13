package io.github.viniciussambinello.stags.infrastructure.authoring;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthoringSession(UUID playerId, AuthoringStep step, Instant lastActivity) {

    public AuthoringSession {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(step, "step");
        Objects.requireNonNull(lastActivity, "lastActivity");
    }

    public AuthoringSession advance(final AuthoringStep newStep, final Instant now) {
        return new AuthoringSession(playerId, newStep, now);
    }
}
