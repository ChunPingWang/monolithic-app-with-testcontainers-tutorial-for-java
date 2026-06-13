package com.tutorial.ecommerce.sharedkernel.domain;

public record Quantity(int value) {

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("quantity must be non-negative: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(value + other.value);
    }

    public Quantity subtract(Quantity other) {
        if (value < other.value) {
            throw new IllegalArgumentException(
                "insufficient quantity: " + value + " < " + other.value);
        }
        return new Quantity(value - other.value);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isAtLeast(Quantity other) {
        return value >= other.value;
    }
}
