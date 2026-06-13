package com.tutorial.ecommerce.sharedkernel.domain;

import java.util.Objects;
import java.util.UUID;

public record PaymentId(UUID value) {
    public PaymentId {
        Objects.requireNonNull(value, "value");
    }

    public static PaymentId newId() {
        return new PaymentId(UUID.randomUUID());
    }
}
