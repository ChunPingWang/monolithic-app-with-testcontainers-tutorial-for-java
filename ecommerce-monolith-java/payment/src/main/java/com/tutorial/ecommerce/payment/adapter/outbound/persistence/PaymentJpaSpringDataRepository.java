package com.tutorial.ecommerce.payment.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PaymentJpaSpringDataRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
