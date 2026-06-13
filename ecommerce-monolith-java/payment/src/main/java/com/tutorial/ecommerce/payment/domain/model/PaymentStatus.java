package com.tutorial.ecommerce.payment.domain.model;

import java.time.Instant;

public sealed interface PaymentStatus
    permits PaymentStatus.Pending, PaymentStatus.Completed, PaymentStatus.Failed, PaymentStatus.Refunded {

    Instant at();

    record Pending(Instant at) implements PaymentStatus {}
    record Completed(Instant at, String receiptObjectKey) implements PaymentStatus {}
    record Failed(Instant at, String reason) implements PaymentStatus {}
    record Refunded(Instant at, String reason) implements PaymentStatus {}
}
