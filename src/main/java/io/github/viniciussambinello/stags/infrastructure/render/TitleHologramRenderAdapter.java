package io.github.viniciussambinello.stags.infrastructure.render;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.infrastructure.config.TitleHologramConfig;

public final class TitleHologramRenderAdapter implements TargetRenderer, Listener {

    private final ConfigService configService;
    private final ActiveCosmeticResolver activeCosmeticResolver;
    private final Plugin plugin;
    private final Server server;
    private final NamespacedKey markerKey;
    private final NamespacedKey ownerKey;
    private final Map<UUID, TextDisplay> holograms;

    public TitleHologramRenderAdapter(
            final ConfigService configService, final ActiveCosmeticResolver activeCosmeticResolver, final Plugin plugin) {
        this.configService = configService;
        this.activeCosmeticResolver = activeCosmeticResolver;
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.markerKey = new NamespacedKey("s-tags", "title-hologram");
        this.ownerKey = new NamespacedKey("s-tags", "title-hologram-owner");
        this.holograms = new ConcurrentHashMap<>();
    }

    @Override
    public void refresh(final Player player) {
        final TitleHologramConfig config = configService.config().render().titleHologram();
        if (!config.enabled()) {
            teardown(player);
            return;
        }

        final Optional<Cosmetic> activeTitle = activeCosmeticResolver.activeCosmetic(player.getUniqueId(), CosmeticKind.TITLE);
        if (activeTitle.isEmpty()) {
            teardown(player);
            return;
        }

        TextDisplay display = holograms.get(player.getUniqueId());
        if (display == null || !display.isValid()) {
            display = spawnHologram(player);
            holograms.put(player.getUniqueId(), display);
        }
        if (!player.getPassengers().contains(display)) {
            player.addPassenger(display);
        }

        display.text(activeTitle.get().prefix().rendered());
        applyOffset(display, config.verticalOffset());
        applyVisibility(player, display, config.selfVisible());
    }

    @Override
    public void teardown(final Player player) {
        final TextDisplay display = holograms.remove(player.getUniqueId());
        if (display != null) {
            display.remove();
        }
    }

    @Override
    public void shutdown() {
        holograms.values().forEach(Entity::remove);
        holograms.clear();
    }

    @Override
    public void onViewerJoin(final Player viewer) {
        final boolean selfVisible = configService.config().render().titleHologram().selfVisible();
        for (final Map.Entry<UUID, TextDisplay> entry : holograms.entrySet()) {
            final Player owner = server.getPlayer(entry.getKey());
            if (owner == null) {
                continue;
            }
            setViewerVisibility(viewer, owner, entry.getValue(), selfVisible);
        }
    }

    @EventHandler
    public void onTeleport(final PlayerTeleportEvent event) {
        rerenderNextTick(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        rerenderNextTick(event.getPlayer());
    }

    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent event) {
        cleanupChunk(event.getChunk());
    }

    private void rerenderNextTick(final Player player) {
        teardown(player);
        final BukkitScheduler scheduler = server.getScheduler();
        scheduler.runTask(plugin, () -> {
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }

    public void cleanupOrphans() {
        for (final World world : server.getWorlds()) {
            for (final Chunk chunk : world.getLoadedChunks()) {
                cleanupChunk(chunk);
            }
        }
    }

    void cleanupChunk(final Chunk chunk) {
        for (final Entity entity : chunk.getEntities()) {
            if (!(entity instanceof TextDisplay display)) {
                continue;
            }
            if (!Boolean.TRUE.equals(display.getPersistentDataContainer().getOrDefault(markerKey, PersistentDataType.BOOLEAN, false))) {
                continue;
            }
            final String rawOwner = display.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            final UUID ownerId = rawOwner != null ? UUID.fromString(rawOwner) : null;
            final boolean tracked = ownerId != null && display.equals(holograms.get(ownerId));
            if (!tracked) {
                display.remove();
            }
        }
    }

    private TextDisplay spawnHologram(final Player player) {
        final TextDisplay display = player.getWorld().spawn(player.getLocation(), TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setSilent(true);
            entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BOOLEAN, true);
            entity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        });
        return display;
    }

    private void applyOffset(final TextDisplay display, final double verticalOffset) {
        final Transformation transformation = new Transformation(
                new Vector3f(0f, (float) verticalOffset, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(1f, 1f, 1f),
                new AxisAngle4f(0f, 0f, 0f, 1f));
        display.setTransformation(transformation);
    }

    private void applyVisibility(final Player owner, final TextDisplay display, final boolean selfVisible) {
        for (final Player viewer : server.getOnlinePlayers()) {
            setViewerVisibility(viewer, owner, display, selfVisible);
        }
    }

    private void setViewerVisibility(final Player viewer, final Player owner, final TextDisplay display, final boolean selfVisible) {
        final boolean shouldSee = viewer.equals(owner) ? selfVisible : viewer.canSee(owner);
        if (shouldSee) {
            viewer.showEntity(plugin, display);
        } else {
            viewer.hideEntity(plugin, display);
        }
    }
}
