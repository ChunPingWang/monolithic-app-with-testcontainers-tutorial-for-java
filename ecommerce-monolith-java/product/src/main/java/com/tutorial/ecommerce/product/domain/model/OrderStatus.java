package com.tutorial.ecommerce.product.domain.model;

import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.time.Instant;

public sealed interface OrderStatus
    permits OrderStatus.Created,
            OrderStatus.Paid,
            OrderStatus.Completed,
            OrderStatus.Cancelled,
            OrderStatus.Refunded {

    Instant at();

    record Created(Instant at) implements OrderStatus {}
    record Paid(Instant at, PaymentId paymentId) implements OrderStatus {}
    record Completed(Instant at) implements OrderStatus {}
    record Cancelled(Instant at, String reason) implements OrderStatus {}
    record Refunded(Instant at, String reason) implements OrderStatus {}
}
