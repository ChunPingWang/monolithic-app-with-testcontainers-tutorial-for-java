package com.tutorial.ecommerce.payment.application.fake;

import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.Payment;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPaymentRepository implements PaymentWriteRepository {

    private final Map<PaymentId, Payment> byId = new ConcurrentHashMap<>();
    private final Map<IdempotencyKey, PaymentId> byKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        byId.put(payment.id(), payment);
        byKey.put(payment.idempotencyKey(), payment.id());
        return payment;
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey key) {
        return Optional.ofNullable(byKey.get(key)).map(byId::get);
    }
}
