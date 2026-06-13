package com.tutorial.ecommerce.payment.application.command;

import com.tutorial.ecommerce.payment.domain.DomainException;
import com.tutorial.ecommerce.payment.domain.port.inbound.RefundPaymentUseCase;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundPaymentCommandHandler implements RefundPaymentUseCase {

    private final PaymentWriteRepository payments;

    public RefundPaymentCommandHandler(PaymentWriteRepository payments) {
        this.payments = payments;
    }

    @Override
    @Transactional
    public void refund(PaymentId paymentId, String reason) {
        var payment = payments.findById(paymentId)
            .orElseThrow(() -> new DomainException("payment not found: " + paymentId));
        payment.refund(reason);
        payments.save(payment);
    }
}
