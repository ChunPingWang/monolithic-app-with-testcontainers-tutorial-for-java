package com.tutorial.ecommerce.payment.application.eventlistener;

import com.tutorial.ecommerce.payment.domain.port.inbound.RefundPaymentUseCase;
import com.tutorial.ecommerce.sharedkernel.event.InventoryDeductionFailedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Saga 補償:庫存扣不到 → 退款。 */
@Component
public class InventoryDeductionFailedEventListener {

    private final RefundPaymentUseCase refund;

    public InventoryDeductionFailedEventListener(RefundPaymentUseCase refund) {
        this.refund = refund;
    }

    @ApplicationModuleListener
    void handle(InventoryDeductionFailedEvent event) {
        refund.refund(event.paymentId(), "compensation:" + event.reason());
    }
}
