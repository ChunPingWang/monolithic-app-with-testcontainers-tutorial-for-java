package com.tutorial.ecommerce.payment.adapter.outbound.persistence;

import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.Payment;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaPaymentRepository implements PaymentWriteRepository {

    private final PaymentJpaSpringDataRepository delegate;

    public JpaPaymentRepository(PaymentJpaSpringDataRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Payment save(Payment payment) {
        var entity = delegate.findById(payment.id().value())
            .map(existing -> { existing.updateFrom(payment); return existing; })
            .orElseGet(() -> PaymentJpaEntity.fromDomain(payment));
        return delegate.save(entity).toDomain();
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return delegate.findById(id.value()).map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey key) {
        return delegate.findByIdempotencyKey(key.value()).map(PaymentJpaEntity::toDomain);
    }
}
