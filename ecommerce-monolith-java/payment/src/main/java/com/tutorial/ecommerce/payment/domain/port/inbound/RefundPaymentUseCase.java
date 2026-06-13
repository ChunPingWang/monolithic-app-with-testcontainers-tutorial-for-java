package com.tutorial.ecommerce.payment.domain.port.inbound;

import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

public interface RefundPaymentUseCase {

    void refund(PaymentId paymentId, String reason);
}
