package io.github.viniciussambinello.stags.infrastructure.authoring;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import io.papermc.paper.event.player.AsyncChatEvent;

import io.github.viniciussambinello.stags.application.port.CosmeticRenderer;
import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.catalogue.CatalogueRules;
import io.github.viniciussambinello.stags.domain.catalogue.FieldValidation;
import io.github.viniciussambinello.stags.domain.catalogue.ValidationError;
import io.github.viniciussambinello.stags.domain.catalogue.ValidationResult;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ChatAuthoringFlow implements Listener {

    private static final String CANCEL_INPUT = "cancel";
    private static final String SKIP_INPUT = "skip";
    private static final String CONFIRM_INPUT = "confirm";

    private final ConfigService configService;
    private final CatalogueService catalogueService;
    private final PlayerCosmeticService playerCosmeticService;
    private final ActiveCosmeticResolver activeCosmeticResolver;
    private final CatalogueRules catalogueRules;
    private final CosmeticRenderer cosmeticRenderer;
    private final AuthoringSessionStore sessionStore;
    private final MainThreadDispatcher dispatcher;
    private final Server server;
    private final Clock clock;

    public ChatAuthoringFlow(
            final ConfigService configService,
            final CatalogueService catalogueService,
            final PlayerCosmeticService playerCosmeticService,
            final ActiveCosmeticResolver activeCosmeticResolver,
            final CosmeticRenderer cosmeticRenderer,
            final AuthoringSessionStore sessionStore,
            final MainThreadDispatcher dispatcher,
            final Server server,
            final Clock clock) {
        this.configService = configService;
        this.catalogueService = catalogueService;
        this.playerCosmeticService = playerCosmeticService;
        this.activeCosmeticResolver = activeCosmeticResolver;
        this.catalogueRules = new CatalogueRules();
        this.cosmeticRenderer = cosmeticRenderer;
        this.sessionStore = sessionStore;
        this.dispatcher = dispatcher;
        this.server = server;
        this.clock = clock;
    }

    public boolean hasActiveSession(final UUID playerId) {
        return sessionStore.find(playerId).isPresent();
    }

    public void startCreate(final Player player, final CosmeticKind kind) {
        sessionStore.start(player.getUniqueId(), new AuthoringStep.CreateAwaitingIdentifier(kind));
        sendMessage(player, "authoring.step-identifier");
    }

    public void startEdit(final Player player, final Cosmetic existing) {
        sessionStore.start(player.getUniqueId(), new AuthoringStep.EditAwaitingPrefix(existing));
        sendMessage(player, "authoring.step-prefix");
    }

    public void startDelete(final Player player, final CosmeticKind kind, final CosmeticId id) {
        sessionStore.start(player.getUniqueId(), new AuthoringStep.DeleteAwaitingConfirmation(kind, id));
        final long onlineUsing = countOnlinePlayersUsing(kind, id);
        sendMessage(player, "authoring.delete-confirm",
                Placeholder.component("id", Component.text(id.value())),
                Placeholder.component("count", Component.text(onlineUsing)));
    }

    public void cancel(final UUID playerId) {
        sessionStore.discard(playerId);
    }

    @EventHandler
    public void onChat(final AsyncChatEvent event) {
        final Player player = event.getPlayer();
        if (sessionStore.find(player.getUniqueId()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        final String rawInput = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        dispatcher.run(() -> handleInput(player, rawInput));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        sessionStore.discard(event.getPlayer().getUniqueId());
    }

    public void sweepExpired() {
        final Duration timeout = Duration.ofSeconds(configService.config().authoring().flowTimeoutSeconds());
        for (final UUID playerId : sessionStore.sweepExpired(timeout)) {
            sendMessageTo(playerId, "authoring.timeout");
        }
    }

    void handleInput(final Player player, final String rawInput) {
        final Optional<AuthoringSession> sessionOptional = sessionStore.find(player.getUniqueId());
        if (sessionOptional.isEmpty()) {
            return;
        }
        final AuthoringSession session = sessionOptional.get();
        if (rawInput.equalsIgnoreCase(CANCEL_INPUT)) {
            sessionStore.discard(player.getUniqueId());
            sendMessage(player, "authoring.cancelled");
            return;
        }

        switch (session.step()) {
            case AuthoringStep.CreateAwaitingIdentifier step -> handleCreateIdentifier(player, session, step, rawInput);
            case AuthoringStep.CreateAwaitingPrefix step -> handleCreatePrefix(player, session, step, rawInput);
            case AuthoringStep.CreateAwaitingPermission step -> handleCreatePermission(player, session, step, rawInput);
            case AuthoringStep.CreateAwaitingWeight step -> handleCreateWeight(player, session, step, rawInput);
            case AuthoringStep.CreateAwaitingConfirmation step -> handleCreateConfirmation(player, step, rawInput);
            case AuthoringStep.EditAwaitingPrefix step -> handleEditPrefix(player, session, step, rawInput);
            case AuthoringStep.EditAwaitingPermission step -> handleEditPermission(player, session, step, rawInput);
            case AuthoringStep.EditAwaitingWeight step -> handleEditWeight(player, session, step, rawInput);
            case AuthoringStep.EditAwaitingConfirmation step -> handleEditConfirmation(player, step, rawInput);
            case AuthoringStep.DeleteAwaitingConfirmation step -> handleDeleteConfirmation(player, step, rawInput);
        }
    }

    private void handleCreateIdentifier(
            final Player player, final AuthoringSession session, final AuthoringStep.CreateAwaitingIdentifier step, final String rawInput) {
        final Catalogue catalogue = catalogueService.catalogue();
        final FieldValidation<CosmeticId> validation = catalogueRules.validateIdentifier(rawInput, step.kind(), catalogue);
        if (validation instanceof FieldValidation.Invalid<CosmeticId> invalid) {
            sendErrorAndRetry(player, invalid.error(), "authoring.step-identifier");
            return;
        }
        final CosmeticId id = ((FieldValidation.Valid<CosmeticId>) validation).value();
        sessionStore.update(session.advance(new AuthoringStep.CreateAwaitingPrefix(step.kind(), id), clock.instant()));
        sendMessage(player, "authoring.step-prefix");
    }

    private void handleCreatePrefix(
            final Player player, final AuthoringSession session, final AuthoringStep.CreateAwaitingPrefix step, final String rawInput) {
        final FieldValidation<Prefix> validation = catalogueRules.validatePrefix(rawInput);
        if (validation instanceof FieldValidation.Invalid<Prefix> invalid) {
            sendErrorAndRetry(player, invalid.error(), "authoring.step-prefix");
            return;
        }
        final Prefix prefix = ((FieldValidation.Valid<Prefix>) validation).value();
        sessionStore.update(session.advance(new AuthoringStep.CreateAwaitingPermission(step.kind(), step.id(), prefix), clock.instant()));
        sendMessage(player, "authoring.step-permission");
    }

    private void handleCreatePermission(
            final Player player, final AuthoringSession session, final AuthoringStep.CreateAwaitingPermission step, final String rawInput) {
        final PermissionNode permission = rawInput.equalsIgnoreCase(SKIP_INPUT)
                ? new PermissionNode(defaultPermission(step.kind(), step.id()))
                : new PermissionNode(rawInput);
        sessionStore.update(session.advance(
                new AuthoringStep.CreateAwaitingWeight(step.kind(), step.id(), step.prefix(), permission), clock.instant()));
        sendMessage(player, "authoring.step-weight");
    }

    private void handleCreateWeight(
            final Player player, final AuthoringSession session, final AuthoringStep.CreateAwaitingWeight step, final String rawInput) {
        final Integer weight = parseWeight(rawInput);
        if (weight == null) {
            sendMessage(player, "authoring.error-invalid-weight");
            return;
        }
        final Cosmetic candidate = new Cosmetic(step.kind(), step.id(), step.prefix(), step.permission(), new Weight(weight));
        sessionStore.update(session.advance(new AuthoringStep.CreateAwaitingConfirmation(candidate), clock.instant()));
        sendPreview(player, candidate);
        sendMessage(player, "authoring.step-confirm");
    }

    private void handleCreateConfirmation(final Player player, final AuthoringStep.CreateAwaitingConfirmation step, final String rawInput) {
        if (!rawInput.equalsIgnoreCase(CONFIRM_INPUT)) {
            sendMessage(player, "authoring.step-confirm");
            return;
        }
        final Cosmetic candidate = step.candidate();
        catalogueService.create(candidate.kind(), candidate.id().value(), candidate.prefix().raw(),
                candidate.permission().value(), candidate.weight().value()).whenComplete((result, throwable) ->
                dispatcher.run(() -> {
                    sessionStore.discard(player.getUniqueId());
                    if (throwable != null) {
                        sendMessage(player, "storage.failure");
                        return;
                    }
                    if (result instanceof ValidationResult.Rejected rejected) {
                        sendErrorMessage(player, rejected.error());
                        return;
                    }
                    sendMessage(player, "authoring.created", Placeholder.component("id", Component.text(candidate.id().value())));
                }));
    }

    private void handleEditPrefix(
            final Player player, final AuthoringSession session, final AuthoringStep.EditAwaitingPrefix step, final String rawInput) {
        final FieldValidation<Prefix> validation = catalogueRules.validatePrefix(rawInput);
        if (validation instanceof FieldValidation.Invalid<Prefix> invalid) {
            sendErrorAndRetry(player, invalid.error(), "authoring.step-prefix");
            return;
        }
        final Prefix prefix = ((FieldValidation.Valid<Prefix>) validation).value();
        sessionStore.update(session.advance(new AuthoringStep.EditAwaitingPermission(step.existing(), prefix), clock.instant()));
        sendMessage(player, "authoring.step-permission");
    }

    private void handleEditPermission(
            final Player player, final AuthoringSession session, final AuthoringStep.EditAwaitingPermission step, final String rawInput) {
        final PermissionNode permission = rawInput.equalsIgnoreCase(SKIP_INPUT)
                ? step.existing().permission()
                : new PermissionNode(rawInput);
        sessionStore.update(session.advance(
                new AuthoringStep.EditAwaitingWeight(step.existing(), step.newPrefix(), permission), clock.instant()));
        sendMessage(player, "authoring.step-weight");
    }

    private void handleEditWeight(
            final Player player, final AuthoringSession session, final AuthoringStep.EditAwaitingWeight step, final String rawInput) {
        final Integer weight = parseWeight(rawInput);
        if (weight == null) {
            sendMessage(player, "authoring.error-invalid-weight");
            return;
        }
        final Cosmetic candidate = step.existing().withPrefix(step.newPrefix()).withPermission(step.newPermission()).withWeight(new Weight(weight));
        sessionStore.update(session.advance(new AuthoringStep.EditAwaitingConfirmation(step.existing(), candidate), clock.instant()));
        sendPreview(player, candidate);
        sendMessage(player, "authoring.step-confirm");
    }

    private void handleEditConfirmation(final Player player, final AuthoringStep.EditAwaitingConfirmation step, final String rawInput) {
        if (!rawInput.equalsIgnoreCase(CONFIRM_INPUT)) {
            sendMessage(player, "authoring.step-confirm");
            return;
        }
        final Cosmetic existing = step.existing();
        final Cosmetic candidate = step.candidate();
        final Optional<CompletableFuture<ValidationResult>> editFuture = catalogueService.edit(
                existing.kind(), existing.id(), candidate.prefix().raw(), candidate.permission().value(), candidate.weight().value());
        if (editFuture.isEmpty()) {
            sessionStore.discard(player.getUniqueId());
            sendMessage(player, "authoring.edit-target-missing");
            return;
        }
        editFuture.get().whenComplete((result, throwable) -> dispatcher.run(() -> {
                    sessionStore.discard(player.getUniqueId());
                    if (throwable != null) {
                        sendMessage(player, "storage.failure");
                        return;
                    }
                    if (result instanceof ValidationResult.Rejected rejected) {
                        sendErrorMessage(player, rejected.error());
                        return;
                    }
                    sendMessage(player, "authoring.edited", Placeholder.component("id", Component.text(existing.id().value())));
                    cosmeticRenderer.refresh(player.getUniqueId());
                }));
    }

    private void handleDeleteConfirmation(final Player player, final AuthoringStep.DeleteAwaitingConfirmation step, final String rawInput) {
        if (!rawInput.equalsIgnoreCase(CONFIRM_INPUT)) {
            sessionStore.discard(player.getUniqueId());
            return;
        }
        sessionStore.discard(player.getUniqueId());
        final List<Player> affected = onlinePlayersUsing(step.kind(), step.id());
        catalogueService.delete(step.kind(), step.id()).whenComplete((unused, throwable) -> dispatcher.run(() -> {
            if (throwable != null) {
                sendMessage(player, "storage.failure");
                return;
            }
            sendMessage(player, "authoring.deleted", Placeholder.component("id", Component.text(step.id().value())));
            for (final Player fallenBack : affected) {
                cosmeticRenderer.refresh(fallenBack.getUniqueId());
                notifyFallback(fallenBack, step.kind());
            }
        }));
    }

    private void notifyFallback(final Player player, final CosmeticKind kind) {
        final Optional<Cosmetic> replacement = activeCosmeticResolver.activeCosmetic(player.getUniqueId(), kind);
        if (kind == CosmeticKind.TAG) {
            if (replacement.isPresent()) {
                sendMessage(player, "selection.tag-fallback", Placeholder.component("name", replacement.get().prefix().rendered()));
            } else {
                sendMessage(player, "selection.tag-fallback-cleared");
            }
        } else {
            sendMessage(player, "selection.title-fallback-cleared");
        }
    }

    private List<Player> onlinePlayersUsing(final CosmeticKind kind, final CosmeticId id) {
        return server.getOnlinePlayers().stream()
                .filter(online -> playerCosmeticService.cached(online.getUniqueId())
                        .map(cosmetics -> cosmetics.selection(kind))
                        .filter(selection -> selection instanceof Selection.Active active && active.cosmeticId().equals(id))
                        .isPresent())
                .map(online -> (Player) online)
                .toList();
    }

    private void sendPreview(final Player player, final Cosmetic candidate) {
        final Component preview = candidate.prefix().rendered()
                .append(Component.space())
                .append(Component.text("(preview, visible only to you)"));
        player.sendMessage(preview);
    }

    private Integer parseWeight(final String rawInput) {
        final int weight;
        try {
            weight = Integer.parseInt(rawInput.trim());
        } catch (final NumberFormatException exception) {
            return null;
        }
        return weight < 0 ? null : weight;
    }

    private String defaultPermission(final CosmeticKind kind, final CosmeticId id) {
        return configService.config().authoring().defaultPermissionPattern()
                .replace("{kind}", kind.name().toLowerCase(Locale.ROOT))
                .replace("{id}", id.value());
    }

    private long countOnlinePlayersUsing(final CosmeticKind kind, final CosmeticId id) {
        return onlinePlayersUsing(kind, id).size();
    }

    private void sendErrorAndRetry(final Player player, final ValidationError error, final String retryKey) {
        sendErrorMessage(player, error);
        sendMessage(player, retryKey);
    }

    private void sendErrorMessage(final Player player, final ValidationError error) {
        switch (error) {
            case ValidationError.DuplicateIdentifier duplicate ->
                    sendMessage(player, "authoring.error-duplicate-identifier",
                            Placeholder.component("id", Component.text(duplicate.id().value())));
            case ValidationError.MalformedIdentifier malformed ->
                    sendMessage(player, "authoring.error-invalid-identifier",
                            Placeholder.component("reason", Component.text(malformed.reason())));
            case ValidationError.MalformedPrefix malformed ->
                    sendMessage(player, "authoring.error-invalid-prefix",
                            Placeholder.component("reason", Component.text(malformed.reason())));
            case ValidationError.InvalidWeight _ -> sendMessage(player, "authoring.error-invalid-weight");
        }
    }

    private void sendMessage(final Player player, final String key, final TagResolver... placeholders) {
        configService.messages().render(key, placeholders).ifPresent(player::sendMessage);
    }

    private void sendMessageTo(final UUID playerId, final String key, final TagResolver... placeholders) {
        final Player player = server.getPlayer(playerId);
        if (player != null) {
            sendMessage(player, key, placeholders);
        }
    }
}
