package io.github.viniciussambinello.stags.application.port;

import java.util.UUID;

import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import net.kyori.adventure.text.Component;

public interface PlaceholderResolver {

    Component resolve(Prefix prefix, UUID wearingPlayerId);
}
