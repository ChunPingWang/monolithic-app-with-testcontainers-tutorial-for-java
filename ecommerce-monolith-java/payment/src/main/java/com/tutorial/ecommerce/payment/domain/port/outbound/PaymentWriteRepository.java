package com.tutorial.ecommerce.payment.domain.port.outbound;

import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.Payment;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.util.Optional;

public interface PaymentWriteRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByIdempotencyKey(IdempotencyKey key);
}
