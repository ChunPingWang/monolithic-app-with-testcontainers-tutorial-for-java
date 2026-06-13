package com.tutorial.ecommerce.inventory.application.eventlistener;

import com.tutorial.ecommerce.inventory.application.OrderLinesLookup;
import com.tutorial.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase;
import com.tutorial.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase.DeductInventoryCommand;
import com.tutorial.ecommerce.sharedkernel.event.PaymentCompletedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 支付完成後扣庫存。
 *
 * 設計細節:PaymentCompletedEvent 只帶 orderId 與金額,沒有 line items。
 * 庫存模組需要透過 OrderLinesLookup 從 commerce.event 系統取出該訂單的 lines。
 * 真實場景可以選擇 (a) 把 lines 放進 PaymentCompletedEvent (b) 庫存模組訂閱 OrderCreatedEvent 並暫存
 * 此教程採 (b),由 OrderLinesLookup port 抽象。
 */
@Component
public class PaymentCompletedEventListener {

    private final DeductInventoryUseCase deduct;
    private final OrderLinesLookup lookup;

    public PaymentCompletedEventListener(DeductInventoryUseCase deduct, OrderLinesLookup lookup) {
        this.deduct = deduct;
        this.lookup = lookup;
    }

    @ApplicationModuleListener
    void handle(PaymentCompletedEvent event) {
        var lines = lookup.findLines(event.orderId());
        deduct.deduct(new DeductInventoryCommand(event.orderId(), event.paymentId(), lines));
    }
}
