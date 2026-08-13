package io.github.viniciussambinello.stags.infrastructure.config;

public record MySqlConfig(
        String host,
        int port,
        String database,
        String username,
        String password,
        String tablePrefix,
        int poolSize,
        FailurePolicy failurePolicy) {
}
