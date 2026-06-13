package com.tutorial.ecommerce.sharedkernel.event;

import com.tutorial.ecommerce.sharedkernel.domain.DomainEvent;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;

import java.time.Instant;
import java.util.List;

/**
 * 商品模組 → 支付模組:訂單已建立,請開始扣款。
 */
public record OrderCreatedEvent(
    OrderId orderId,
    UserId buyerId,
    List<OrderLineItem> lines,
    Money totalAmount,
    Instant occurredAt
) implements DomainEvent {

    public OrderCreatedEvent {
        lines = List.copyOf(lines);
    }
}
