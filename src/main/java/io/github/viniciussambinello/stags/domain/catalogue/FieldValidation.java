package io.github.viniciussambinello.stags.domain.catalogue;

public sealed interface FieldValidation<T> {

    record Valid<T>(T value) implements FieldValidation<T> {
    }

    record Invalid<T>(ValidationError error) implements FieldValidation<T> {
    }
}
