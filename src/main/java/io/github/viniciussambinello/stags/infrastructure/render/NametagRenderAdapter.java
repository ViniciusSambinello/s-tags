package io.github.viniciussambinello.stags.infrastructure.render;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;

public final class NametagRenderAdapter implements TargetRenderer {

    private static final String TEAM_PREFIX = "st_";

    private final ConfigService configService;
    private final ActiveCosmeticResolver activeCosmeticResolver;
    private final Server server;
    private final Map<String, Team> managedTeams;

    public NametagRenderAdapter(
            final ConfigService configService, final ActiveCosmeticResolver activeCosmeticResolver, final Server server) {
        this.configService = configService;
        this.activeCosmeticResolver = activeCosmeticResolver;
        this.server = server;
        this.managedTeams = new ConcurrentHashMap<>();
    }

    private Scoreboard scoreboard() {
        return server.getScoreboardManager().getMainScoreboard();
    }

    @Override
    public void refresh(final Player player) {
        if (!configService.config().render().nametagEnabled()) {
            teardown(player);
            return;
        }
        final Optional<Cosmetic> activeTag = activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TAG);
        removeFromManagedTeam(player);
        activeTag.ifPresent(tag -> teamFor(tag).addEntry(player.getName()));
    }

    @Override
    public void teardown(final Player player) {
        removeFromManagedTeam(player);
    }

    @Override
    public void shutdown() {
        managedTeams.values().forEach(Team::unregister);
        managedTeams.clear();
    }

    private void removeFromManagedTeam(final Player player) {
        final Team current = scoreboard().getEntryTeam(player.getName());
        if (current != null && current.getName().startsWith(TEAM_PREFIX) && managedTeams.containsValue(current)) {
            current.removeEntry(player.getName());
        }
    }

    private Team teamFor(final Cosmetic tag) {
        final String name = teamName(tag);
        final Team team = managedTeams.computeIfAbsent(name, key -> {
            final Scoreboard scoreboard = scoreboard();
            final Team existing = scoreboard.getTeam(key);
            return existing != null ? existing : scoreboard.registerNewTeam(key);
        });
        team.prefix(tag.prefix().rendered());
        return team;
    }

    private String teamName(final Cosmetic tag) {
        final String hash = Long.toString(Math.abs((long) tag.id().value().hashCode()), 36);
        final String base = TEAM_PREFIX + hash;
        return base.length() > 16 ? base.substring(0, 16) : base;
    }
}
