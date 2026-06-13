package com.tutorial.ecommerce.product.domain;

import com.tutorial.ecommerce.product.domain.event.OrderDomainEvent;
import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.model.OrderLine;
import com.tutorial.ecommerce.product.domain.model.OrderStatus;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final UserId buyer = new UserId("buyer01");
    private final OrderLine line = new OrderLine(ProductId.newId(), Quantity.of(2), Money.of("100.00", "TWD"));

    @Test
    void create_initialStatusIsCreated_andTotalIsLineSum() {
        var order = Order.create(buyer, List.of(line));

        assertThat(order.status()).isInstanceOf(OrderStatus.Created.class);
        assertThat(order.totalAmount()).isEqualTo(Money.of("200.00", "TWD"));
    }

    @Test
    void create_emptyLines_throws() {
        assertThatThrownBy(() -> Order.create(buyer, List.of()))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("at least one line");
    }

    @Test
    void create_emitsOrderCreatedDomainEvent() {
        var order = Order.create(buyer, List.of(line));

        var events = order.drainEvents();
        assertThat(events).hasSize(1)
            .first().isInstanceOf(OrderDomainEvent.OrderCreated.class);
    }

    @Test
    void markPaid_fromCreated_transitionsToPaid() {
        var order = Order.create(buyer, List.of(line));
        order.drainEvents();
        var paymentId = PaymentId.newId();

        order.markPaid(paymentId);

        assertThat(order.status()).isInstanceOf(OrderStatus.Paid.class);
        var events = order.drainEvents();
        assertThat(events).hasSize(1)
            .first().isInstanceOf(OrderDomainEvent.OrderPaid.class);
    }

    @Test
    void markPaid_fromNonCreated_throws() {
        var order = Order.create(buyer, List.of(line));
        order.markPaid(PaymentId.newId());

        assertThatThrownBy(() -> order.markPaid(PaymentId.newId()))
            .isInstanceOf(DomainException.class);
    }

    @Test
    void markCompleted_fromPaid_transitionsToCompleted() {
        var order = Order.create(buyer, List.of(line));
        order.markPaid(PaymentId.newId());
        order.drainEvents();

        order.markCompleted();

        assertThat(order.status()).isInstanceOf(OrderStatus.Completed.class);
    }

    @Test
    void markCompleted_fromCreated_throws() {
        var order = Order.create(buyer, List.of(line));

        assertThatThrownBy(order::markCompleted)
            .isInstanceOf(DomainException.class);
    }

    @Test
    void cancel_recordsReason() {
        var order = Order.create(buyer, List.of(line));
        order.drainEvents();

        order.cancel("user changed mind");

        assertThat(order.status()).isInstanceOfSatisfying(OrderStatus.Cancelled.class,
            c -> assertThat(c.reason()).isEqualTo("user changed mind"));
    }

    @Test
    void cancel_afterCompleted_throws() {
        var order = Order.create(buyer, List.of(line));
        order.markPaid(PaymentId.newId());
        order.markCompleted();

        assertThatThrownBy(() -> order.cancel("late"))
            .isInstanceOf(DomainException.class);
    }
}
