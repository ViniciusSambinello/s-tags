package io.github.viniciussambinello.stags.infrastructure.authoring;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthoringSessionStore {

    private final Clock clock;
    private final Map<UUID, AuthoringSession> sessions;

    public AuthoringSessionStore(final Clock clock) {
        this.clock = clock;
        this.sessions = new ConcurrentHashMap<>();
    }

    public Optional<AuthoringSession> find(final UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void start(final UUID playerId, final AuthoringStep initialStep) {
        sessions.put(playerId, new AuthoringSession(playerId, initialStep, clock.instant()));
    }

    public void update(final AuthoringSession session) {
        sessions.put(session.playerId(), session);
    }

    public void discard(final UUID playerId) {
        sessions.remove(playerId);
    }

    public List<UUID> sweepExpired(final Duration timeout) {
        final List<UUID> expired = sessions.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue().lastActivity(), clock.instant()).compareTo(timeout) > 0)
                .map(Map.Entry::getKey)
                .toList();
        expired.forEach(sessions::remove);
        return expired;
    }
}
