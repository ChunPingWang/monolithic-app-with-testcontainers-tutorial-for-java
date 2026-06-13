package com.tutorial.ecommerce.sharedkernel.domain;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {
    public ProductId {
        Objects.requireNonNull(value, "value");
    }

    public static ProductId newId() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(String value) {
        return new ProductId(UUID.fromString(value));
    }
}
