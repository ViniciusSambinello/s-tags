package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.configuration.ConfigurationSection;

final class ConfigValueReader {

    private final ConfigurationSection root;
    private final List<String> warnings = new ArrayList<>();

    ConfigValueReader(final ConfigurationSection root) {
        this.root = root;
    }

    List<String> warnings() {
        return List.copyOf(warnings);
    }

    void warn(final String message) {
        warnings.add(message);
    }

    String getString(final String path, final String defaultValue) {
        if (!root.isString(path)) {
            warnIfPresent(path, defaultValue, "a text value");
            return defaultValue;
        }
        return root.getString(path, defaultValue);
    }

    boolean getBoolean(final String path, final boolean defaultValue) {
        if (!root.isBoolean(path)) {
            warnIfPresent(path, defaultValue, "true or false");
            return defaultValue;
        }
        return root.getBoolean(path, defaultValue);
    }

    int getInt(final String path, final int defaultValue, final int min, final int max) {
        if (!root.isInt(path)) {
            warnIfPresent(path, defaultValue, "a whole number");
            return defaultValue;
        }
        final int value = root.getInt(path, defaultValue);
        if (value < min || value > max) {
            warnings.add("Value for '" + path + "' (" + value + ") is outside the accepted range ["
                    + min + ", " + max + "]. Using default " + defaultValue + ".");
            return defaultValue;
        }
        return value;
    }

    double getDouble(final String path, final double defaultValue, final double min, final double max) {
        if (!root.isDouble(path) && !root.isInt(path)) {
            warnIfPresent(path, defaultValue, "a number");
            return defaultValue;
        }
        final double value = root.getDouble(path, defaultValue);
        if (value < min || value > max) {
            warnings.add("Value for '" + path + "' (" + value + ") is outside the accepted range ["
                    + min + ", " + max + "]. Using default " + defaultValue + ".");
            return defaultValue;
        }
        return value;
    }

    List<Integer> getIntList(final String path, final List<Integer> defaultValue) {
        if (!root.isList(path)) {
            warnIfPresent(path, defaultValue, "a list of whole numbers");
            return defaultValue;
        }
        try {
            return List.copyOf(root.getIntegerList(path));
        } catch (final RuntimeException exception) {
            warnings.add("Value for '" + path + "' could not be read as a list of whole numbers. Using default.");
            return defaultValue;
        }
    }

    <T extends Enum<T>> T getEnum(final String path, final Class<T> type, final T defaultValue) {
        if (!root.isString(path)) {
            warnIfPresent(path, defaultValue, acceptedEnumValues(type));
            return defaultValue;
        }
        final String raw = root.getString(path, "");
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            warnings.add("Value for '" + path + "' ('" + raw + "') is not one of " + acceptedEnumValues(type)
                    + ". Using default " + defaultValue + ".");
            return defaultValue;
        }
    }

    private <T extends Enum<T>> String acceptedEnumValues(final Class<T> type) {
        final StringBuilder builder = new StringBuilder("[");
        final T[] constants = type.getEnumConstants();
        for (int index = 0; index < constants.length; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(constants[index].name());
        }
        return builder.append(']').toString();
    }

    private void warnIfPresent(final String path, final Object defaultValue, final String expected) {
        if (root.contains(path)) {
            warnings.add("Invalid type for '" + path + "': expected " + expected + ", got '"
                    + root.get(path) + "'. Using default " + defaultValue + ".");
        }
    }
}
