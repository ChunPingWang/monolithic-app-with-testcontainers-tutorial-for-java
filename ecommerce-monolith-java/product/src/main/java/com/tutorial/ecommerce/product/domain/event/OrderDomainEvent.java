package com.tutorial.ecommerce.product.domain.event;

import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.time.Instant;

/**
 * 模組內部 Domain Event(不跨模組,只描述聚合狀態變化)。
 * 跨模組通訊請用 shared-kernel.event 下的 Integration Event。
 */
public sealed interface OrderDomainEvent
    permits OrderDomainEvent.OrderCreated,
            OrderDomainEvent.OrderPaid,
            OrderDomainEvent.OrderCompleted,
            OrderDomainEvent.OrderCancelled {

    OrderId orderId();
    Instant occurredAt();

    record OrderCreated(OrderId orderId, Instant occurredAt) implements OrderDomainEvent {}
    record OrderPaid(OrderId orderId, PaymentId paymentId, Instant occurredAt) implements OrderDomainEvent {}
    record OrderCompleted(OrderId orderId, Instant occurredAt) implements OrderDomainEvent {}
    record OrderCancelled(OrderId orderId, String reason, Instant occurredAt) implements OrderDomainEvent {}
}
