package io.github.viniciussambinello.stags.domain.cosmetic;

public record Weight(int value) implements Comparable<Weight> {

    public static Weight zero() {
        return new Weight(0);
    }

    @Override
    public int compareTo(final Weight other) {
        return Integer.compare(value, other.value);
    }
}
