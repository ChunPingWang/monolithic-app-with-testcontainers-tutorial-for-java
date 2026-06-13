package com.tutorial.ecommerce.sharedkernel.event;

import com.tutorial.ecommerce.sharedkernel.domain.DomainEvent;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.time.Instant;

/**
 * 支付模組 → 商品模組:扣款失敗,請取消訂單(補償)。
 */
public record PaymentFailedEvent(
    PaymentId paymentId,
    OrderId orderId,
    String reason,
    Instant occurredAt
) implements DomainEvent {}
