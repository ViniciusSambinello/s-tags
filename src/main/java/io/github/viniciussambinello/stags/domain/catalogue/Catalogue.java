package io.github.viniciussambinello.stags.domain.catalogue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.CosmeticOwnership;

public final class Catalogue {

    private static final Comparator<Cosmetic> ORDER =
            Comparator.comparing(Cosmetic::weight, Comparator.reverseOrder())
                    .thenComparing(Cosmetic::id);

    private final Map<CosmeticKind, List<Cosmetic>> ordered;
    private final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> indexed;

    private Catalogue(
            final Map<CosmeticKind, List<Cosmetic>> ordered,
            final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> indexed) {
        this.ordered = ordered;
        this.indexed = indexed;
    }

    public static Catalogue empty() {
        final Map<CosmeticKind, List<Cosmetic>> ordered = new EnumMap<>(CosmeticKind.class);
        final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> indexed = new EnumMap<>(CosmeticKind.class);
        for (final CosmeticKind kind : CosmeticKind.values()) {
            ordered.put(kind, List.of());
            indexed.put(kind, Map.of());
        }
        return new Catalogue(Map.copyOf(ordered), Map.copyOf(indexed));
    }

    public static Catalogue of(final Collection<Cosmetic> cosmetics) {
        Catalogue catalogue = empty();
        for (final Cosmetic cosmetic : cosmetics) {
            catalogue = catalogue.withCosmetic(cosmetic);
        }
        return catalogue;
    }

    public List<Cosmetic> all(final CosmeticKind kind) {
        return ordered.getOrDefault(kind, List.of());
    }

    public Optional<Cosmetic> find(final CosmeticKind kind, final CosmeticId id) {
        return Optional.ofNullable(indexed.getOrDefault(kind, Map.of()).get(id));
    }

    public boolean contains(final CosmeticKind kind, final CosmeticId id) {
        return indexed.getOrDefault(kind, Map.of()).containsKey(id);
    }

    public List<Cosmetic> ownedBy(final CosmeticKind kind, final CosmeticOwnership ownership) {
        return all(kind).stream().filter(cosmetic -> ownership.owns(cosmetic.id())).toList();
    }

    public Catalogue withCosmetic(final Cosmetic cosmetic) {
        final Map<CosmeticId, Cosmetic> existingIndex = indexed.getOrDefault(cosmetic.kind(), Map.of());
        final Map<CosmeticId, Cosmetic> newIndex = new HashMap<>(existingIndex);
        newIndex.put(cosmetic.id(), cosmetic);

        final List<Cosmetic> newOrder = new ArrayList<>(newIndex.values());
        newOrder.sort(ORDER);

        final Map<CosmeticKind, List<Cosmetic>> updatedOrdered = new EnumMap<>(ordered);
        updatedOrdered.put(cosmetic.kind(), List.copyOf(newOrder));

        final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> updatedIndexed = new EnumMap<>(indexed);
        updatedIndexed.put(cosmetic.kind(), Map.copyOf(newIndex));

        return new Catalogue(Map.copyOf(updatedOrdered), Map.copyOf(updatedIndexed));
    }

    public Catalogue withoutCosmetic(final CosmeticKind kind, final CosmeticId id) {
        final Map<CosmeticId, Cosmetic> existingIndex = indexed.getOrDefault(kind, Map.of());
        if (!existingIndex.containsKey(id)) {
            return this;
        }

        final Map<CosmeticId, Cosmetic> newIndex = new HashMap<>(existingIndex);
        newIndex.remove(id);

        final List<Cosmetic> newOrder = all(kind).stream()
                .filter(existing -> !existing.id().equals(id))
                .toList();

        final Map<CosmeticKind, List<Cosmetic>> updatedOrdered = new EnumMap<>(ordered);
        updatedOrdered.put(kind, newOrder);

        final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> updatedIndexed = new EnumMap<>(indexed);
        updatedIndexed.put(kind, Map.copyOf(newIndex));

        return new Catalogue(Map.copyOf(updatedOrdered), Map.copyOf(updatedIndexed));
    }
}
