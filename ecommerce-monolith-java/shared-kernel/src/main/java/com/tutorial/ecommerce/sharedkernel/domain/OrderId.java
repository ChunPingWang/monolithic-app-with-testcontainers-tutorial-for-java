package com.tutorial.ecommerce.sharedkernel.domain;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }
}
