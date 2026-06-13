package com.tutorial.ecommerce.sharedkernel.domain;

import java.util.Objects;

public record UserId(String value) {
    public UserId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }
}
