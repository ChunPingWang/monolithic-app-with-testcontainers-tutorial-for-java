package com.tutorial.ecommerce.payment.domain.port.inbound;

import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

public interface ProcessPaymentUseCase {

    PaymentId process(ProcessPaymentCommand command);

    record ProcessPaymentCommand(OrderId orderId, Money amount, String idempotencyKey) {}
}
