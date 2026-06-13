package com.tutorial.ecommerce.payment.adapter.outbound.persistence;

import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.Payment;
import com.tutorial.ecommerce.payment.domain.model.PaymentStatus;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment")
class PaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "status_kind", length = 20)
    private String statusKind;

    @Column(name = "status_at")
    private Instant statusAt;

    @Column(name = "status_receipt_key")
    private String statusReceiptKey;

    @Column(name = "status_reason")
    private String statusReason;

    @Version
    private Long version;

    protected PaymentJpaEntity() {}

    static PaymentJpaEntity fromDomain(Payment p) {
        var e = new PaymentJpaEntity();
        e.id = p.id().value();
        e.orderId = p.orderId().value();
        e.amount = p.amount().amount();
        e.currency = p.amount().currency().getCurrencyCode();
        e.idempotencyKey = p.idempotencyKey().value();
        e.applyStatus(p.status());
        e.version = p.version();
        return e;
    }

    void updateFrom(Payment p) {
        applyStatus(p.status());
    }

    private void applyStatus(PaymentStatus status) {
        statusAt = status.at();
        statusReceiptKey = null;
        statusReason = null;
        switch (status) {
            case PaymentStatus.Pending p -> statusKind = "PENDING";
            case PaymentStatus.Completed c -> { statusKind = "COMPLETED"; statusReceiptKey = c.receiptObjectKey(); }
            case PaymentStatus.Failed f -> { statusKind = "FAILED"; statusReason = f.reason(); }
            case PaymentStatus.Refunded r -> { statusKind = "REFUNDED"; statusReason = r.reason(); }
        }
    }

    Payment toDomain() {
        var status = switch (statusKind) {
            case "PENDING"   -> new PaymentStatus.Pending(statusAt);
            case "COMPLETED" -> new PaymentStatus.Completed(statusAt, statusReceiptKey);
            case "FAILED"    -> new PaymentStatus.Failed(statusAt, statusReason);
            case "REFUNDED"  -> new PaymentStatus.Refunded(statusAt, statusReason);
            default -> throw new IllegalStateException("unknown status: " + statusKind);
        };
        return Payment.rehydrate(
            new PaymentId(id),
            new OrderId(orderId),
            new Money(amount, Currency.getInstance(currency)),
            new IdempotencyKey(idempotencyKey),
            status,
            version == null ? 0 : version
        );
    }

    UUID getId() { return id; }
    String getIdempotencyKey() { return idempotencyKey; }
}
