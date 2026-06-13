package com.tutorial.ecommerce.inventory.application;

import com.tutorial.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase.Line;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;

import java.util.List;

/**
 * 庫存模組需要從訂單事件流取出某訂單的 lines。
 * 這個 port 的實作會監聽 OrderCreatedEvent 並暫存 lines 直到 PaymentCompletedEvent 觸發。
 */
public interface OrderLinesLookup {

    List<Line> findLines(OrderId orderId);
}
