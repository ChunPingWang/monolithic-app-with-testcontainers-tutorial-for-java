package com.tutorial.ecommerce.sharedkernel.event;

import com.tutorial.ecommerce.sharedkernel.domain.DomainEvent;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.time.Instant;

/**
 * 支付模組 → 庫存模組:款項已收,請扣庫存。
 */
public record PaymentCompletedEvent(
    PaymentId paymentId,
    OrderId orderId,
    Money paidAmount,
    String receiptObjectKey,
    Instant occurredAt
) implements DomainEvent {}
