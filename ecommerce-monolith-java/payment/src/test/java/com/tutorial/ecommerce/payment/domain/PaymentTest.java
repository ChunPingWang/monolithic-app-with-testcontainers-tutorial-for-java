package com.tutorial.ecommerce.payment.domain;

import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.Payment;
import com.tutorial.ecommerce.payment.domain.model.PaymentStatus;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private final OrderId orderId = OrderId.newId();
    private final Money amount = Money.of("1000.00", "TWD");
    private final IdempotencyKey key = new IdempotencyKey("order-" + orderId.value());

    @Test
    void initiate_setsPendingStatus() {
        var p = Payment.initiate(orderId, amount, key);
        assertThat(p.status()).isInstanceOf(PaymentStatus.Pending.class);
    }

    @Test
    void markCompleted_fromPending_transitionsToCompleted() {
        var p = Payment.initiate(orderId, amount, key);
        p.markCompleted("receipts/r1.pdf");

        assertThat(p.status()).isInstanceOfSatisfying(PaymentStatus.Completed.class,
            c -> assertThat(c.receiptObjectKey()).isEqualTo("receipts/r1.pdf"));
    }

    @Test
    void markCompleted_twice_throws() {
        var p = Payment.initiate(orderId, amount, key);
        p.markCompleted("r");
        assertThatThrownBy(() -> p.markCompleted("r"))
            .isInstanceOf(DomainException.class);
    }

    @Test
    void refund_onlyCompletedCanRefund() {
        var p = Payment.initiate(orderId, amount, key);
        assertThatThrownBy(() -> p.refund("nope")).isInstanceOf(DomainException.class);
        p.markCompleted("r");
        p.refund("user request");
        assertThat(p.status()).isInstanceOf(PaymentStatus.Refunded.class);
    }
}
