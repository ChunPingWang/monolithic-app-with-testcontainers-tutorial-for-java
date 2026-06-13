package com.tutorial.ecommerce.payment.domain.model;

import java.util.Objects;

public record IdempotencyKey(String value) {
    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("idempotency key must not be blank");
    }
}
