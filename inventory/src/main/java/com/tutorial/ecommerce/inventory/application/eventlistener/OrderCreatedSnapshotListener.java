package com.tutorial.ecommerce.inventory.application.eventlistener;

import com.tutorial.ecommerce.inventory.application.OrderLinesLookup;
import com.tutorial.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase.Line;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.event.OrderCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 訂單建立時把 lines 暫存,供 PaymentCompletedEventListener 後續扣庫存使用。
 * 同時實作 OrderLinesLookup port。
 *
 * 此為進程內快取,正式環境可改為 Redis 或單獨 DB 表。
 */
@Component
public class OrderCreatedSnapshotListener implements OrderLinesLookup {

    private final Map<OrderId, List<Line>> snapshot = new ConcurrentHashMap<>();

    @ApplicationModuleListener
    void handle(OrderCreatedEvent event) {
        var lines = event.lines().stream()
            .map(l -> new Line(l.productId(), l.quantity()))
            .toList();
        snapshot.put(event.orderId(), lines);
    }

    @Override
    public List<Line> findLines(OrderId orderId) {
        var lines = snapshot.get(orderId);
        if (lines == null) {
            throw new IllegalStateException("no order snapshot for " + orderId.value());
        }
        return lines;
    }
}
