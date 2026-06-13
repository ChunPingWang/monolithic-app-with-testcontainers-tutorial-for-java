package com.tutorial.ecommerce.payment.domain.model;

import com.tutorial.ecommerce.payment.domain.DomainException;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;

import java.time.Instant;

public class Payment {

    private final PaymentId id;
    private final OrderId orderId;
    private final Money amount;
    private final IdempotencyKey idempotencyKey;
    private PaymentStatus status;
    private long version;

    private Payment(PaymentId id, OrderId orderId, Money amount, IdempotencyKey key,
                    PaymentStatus status, long version) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.idempotencyKey = key;
        this.status = status;
        this.version = version;
    }

    public static Payment initiate(OrderId orderId, Money amount, IdempotencyKey key) {
        if (amount.isNegative()) throw new DomainException("amount must be non-negative");
        return new Payment(PaymentId.newId(), orderId, amount, key, new PaymentStatus.Pending(Instant.now()), 0L);
    }

    public static Payment rehydrate(PaymentId id, OrderId orderId, Money amount, IdempotencyKey key,
                                    PaymentStatus status, long version) {
        return new Payment(id, orderId, amount, key, status, version);
    }

    public void markCompleted(String receiptObjectKey) {
        if (!(status instanceof PaymentStatus.Pending)) {
            throw new DomainException("can only complete a PENDING payment, current=" + status);
        }
        this.status = new PaymentStatus.Completed(Instant.now(), receiptObjectKey);
    }

    public void markFailed(String reason) {
        if (!(status instanceof PaymentStatus.Pending)) {
            throw new DomainException("can only fail a PENDING payment, current=" + status);
        }
        this.status = new PaymentStatus.Failed(Instant.now(), reason);
    }

    public void refund(String reason) {
        if (!(status instanceof PaymentStatus.Completed)) {
            throw new DomainException("only COMPLETED payments can be refunded, current=" + status);
        }
        this.status = new PaymentStatus.Refunded(Instant.now(), reason);
    }

    public PaymentId id() { return id; }
    public OrderId orderId() { return orderId; }
    public Money amount() { return amount; }
    public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    public PaymentStatus status() { return status; }
    public long version() { return version; }
}
