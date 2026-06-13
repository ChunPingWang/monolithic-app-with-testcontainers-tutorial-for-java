package com.tutorial.ecommerce.sharedkernel.event;

import com.tutorial.ecommerce.sharedkernel.domain.DomainEvent;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;

import java.time.Instant;

/**
 * 庫存模組 → 商品模組:庫存已扣減成功,請把訂單標記 COMPLETED。
 */
public record InventoryDeductedEvent(
    OrderId orderId,
    Instant occurredAt
) implements DomainEvent {}
