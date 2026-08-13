package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class MessageCatalog {

    private static final MiniMessage RENDER_PARSER = MiniMessage.miniMessage();

    private static final MiniMessage STRUCTURAL_VALIDATOR = MiniMessage.builder().strict(true).build();

    private final Component prefix;
    private final Set<String> unprefixedKeys;
    private final Map<String, String> rawMessages;

    private MessageCatalog(final Component prefix, final Set<String> unprefixedKeys, final Map<String, String> rawMessages) {
        this.prefix = prefix;
        this.unprefixedKeys = Set.copyOf(unprefixedKeys);
        this.rawMessages = Map.copyOf(rawMessages);
    }

    public record LoadResult(MessageCatalog catalog, List<String> warnings) {
    }

    public static LoadResult load(final YamlConfiguration live, final YamlConfiguration shippedDefault) {
        final List<String> warnings = new ArrayList<>();

        final String defaultRawPrefix = shippedDefault.getString("prefix", "");
        final String candidatePrefix = live.isString("prefix") ? live.getString("prefix", defaultRawPrefix) : defaultRawPrefix;
        final Component prefix = parseOrFallback("prefix", candidatePrefix, defaultRawPrefix, warnings);

        final Set<String> unprefixedKeys = new LinkedHashSet<>(live.getStringList("unprefixed-messages"));

        final Map<String, String> defaultFlat = flatten(shippedDefault.getConfigurationSection("messages"));
        final Map<String, String> liveFlat = flatten(live.getConfigurationSection("messages"));

        final Map<String, String> resolved = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : defaultFlat.entrySet()) {
            final String key = entry.getKey();
            final String defaultRaw = entry.getValue();
            final String candidate = liveFlat.getOrDefault(key, defaultRaw);
            if (isStructurallyValid(candidate)) {
                resolved.put(key, candidate);
            } else {
                warnings.add("Message '" + key + "' failed to parse as MiniMessage, using the shipped default.");
                resolved.put(key, defaultRaw);
            }
        }

        return new LoadResult(new MessageCatalog(prefix, unprefixedKeys, resolved), List.copyOf(warnings));
    }

    public Optional<Component> render(final String key, final TagResolver... placeholders) {
        final String raw = rawMessages.get(key);
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        final Component body = RENDER_PARSER.deserialize(raw, placeholders);
        if (unprefixedKeys.contains(key)) {
            return Optional.of(body);
        }
        return Optional.of(prefix.append(body));
    }

    private static Component parseOrFallback(
            final String key, final String candidate, final String fallbackRaw, final List<String> warnings) {
        if (isStructurallyValid(candidate)) {
            return RENDER_PARSER.deserialize(candidate);
        }
        warnings.add("Message '" + key + "' failed to parse as MiniMessage, using the shipped default.");
        return RENDER_PARSER.deserialize(fallbackRaw);
    }

    private static boolean isStructurallyValid(final String raw) {
        try {
            STRUCTURAL_VALIDATOR.deserialize(raw);
            return true;
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    private static Map<String, String> flatten(final ConfigurationSection section) {
        final Map<String, String> flattened = new LinkedHashMap<>();
        if (section == null) {
            return flattened;
        }
        collect(section, flattened);
        return flattened;
    }

    private static void collect(final ConfigurationSection section, final Map<String, String> sink) {
        for (final String key : section.getKeys(false)) {
            final Object value = section.get(key);
            final String path = section.getCurrentPath().isEmpty() ? key : section.getCurrentPath() + "." + key;
            if (value instanceof ConfigurationSection nested) {
                collect(nested, sink);
            } else if (value != null) {
                sink.put(stripMessagesPrefix(path), String.valueOf(value));
            }
        }
    }

    private static String stripMessagesPrefix(final String path) {
        return path.startsWith("messages.") ? path.substring("messages.".length()) : path;
    }
}
