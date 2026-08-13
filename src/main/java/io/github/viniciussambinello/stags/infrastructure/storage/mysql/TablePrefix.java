package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import java.util.regex.Pattern;

final class TablePrefix {

    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_]*");

    static String validate(final String rawPrefix) {
        if (!ALLOWED.matcher(rawPrefix).matches()) {
            throw new IllegalArgumentException(
                    "storage.mysql.table-prefix '" + rawPrefix + "' must only contain letters, digits and underscore");
        }
        return rawPrefix;
    }

    private TablePrefix() {
    }
}
