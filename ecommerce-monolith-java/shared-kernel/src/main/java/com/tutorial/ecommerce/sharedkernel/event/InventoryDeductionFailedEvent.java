package com.tutorial.ecommerce.sharedkernel.event;

import com.tutorial.ecommerce.sharedkernel.domain.DomainEvent;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.time.Instant;

/**
 * 庫存模組 → 支付模組:庫存不足,請發起退款(補償)。
 */
public record InventoryDeductionFailedEvent(
    OrderId orderId,
    PaymentId paymentId,
    String reason,
    Instant occurredAt
) implements DomainEvent {}
