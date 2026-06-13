package com.tutorial.ecommerce.payment.application.eventlistener;

import com.tutorial.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.tutorial.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase.ProcessPaymentCommand;
import com.tutorial.ecommerce.sharedkernel.event.OrderCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 商品模組訂單建立後,觸發扣款。
 *
 * @ApplicationModuleListener 等同 @TransactionalEventListener(phase=AFTER_COMMIT) + @Async + @Transactional,
 * 並與 Spring Modulith 的 EventPublication Log 整合 — 失敗時可重發,確保 at-least-once。
 */
@Component
public class OrderCreatedEventListener {

    private final ProcessPaymentUseCase processPayment;

    public OrderCreatedEventListener(ProcessPaymentUseCase processPayment) {
        this.processPayment = processPayment;
    }

    @ApplicationModuleListener
    void handle(OrderCreatedEvent event) {
        processPayment.process(new ProcessPaymentCommand(
            event.orderId(),
            event.totalAmount(),
            "order-" + event.orderId().value()
        ));
    }
}
