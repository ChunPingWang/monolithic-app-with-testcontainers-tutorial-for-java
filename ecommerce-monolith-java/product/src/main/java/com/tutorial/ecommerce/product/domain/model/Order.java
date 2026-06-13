package com.tutorial.ecommerce.product.domain.model;

import com.tutorial.ecommerce.product.domain.DomainException;
import com.tutorial.ecommerce.product.domain.event.OrderDomainEvent;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 訂單 Aggregate Root(純 POJO,不依賴 Spring/JPA)。
 * Domain Event 在 internal list 累積,由 application 層 drain 後 publish。
 */
public class Order {

    private final OrderId id;
    private final UserId buyerId;
    private final List<OrderLine> lines;
    private final Money totalAmount;
    private OrderStatus status;
    private long version;
    private final List<OrderDomainEvent> domainEvents = new ArrayList<>();

    private Order(OrderId id, UserId buyerId, List<OrderLine> lines, Money totalAmount, OrderStatus status, long version) {
        this.id = id;
        this.buyerId = buyerId;
        this.lines = lines;
        this.totalAmount = totalAmount;
        this.status = status;
        this.version = version;
    }

    public static Order create(UserId buyerId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new DomainException("order must have at least one line");
        }
        var id = OrderId.newId();
        var now = Instant.now();
        var total = sumOf(lines);
        var order = new Order(id, buyerId, List.copyOf(lines), total, new OrderStatus.Created(now), 0L);
        order.domainEvents.add(new OrderDomainEvent.OrderCreated(id, now));
        return order;
    }

    /** Repository 重建用。 */
    public static Order rehydrate(OrderId id, UserId buyerId, List<OrderLine> lines,
                                  Money totalAmount, OrderStatus status, long version) {
        return new Order(id, buyerId, List.copyOf(lines), totalAmount, status, version);
    }

    public void markPaid(PaymentId paymentId) {
        if (!(status instanceof OrderStatus.Created)) {
            throw new DomainException("only CREATED orders can be marked paid, current=" + status);
        }
        var now = Instant.now();
        status = new OrderStatus.Paid(now, paymentId);
        domainEvents.add(new OrderDomainEvent.OrderPaid(id, paymentId, now));
    }

    public void markCompleted() {
        if (!(status instanceof OrderStatus.Paid)) {
            throw new DomainException("only PAID orders can be completed, current=" + status);
        }
        var now = Instant.now();
        status = new OrderStatus.Completed(now);
        domainEvents.add(new OrderDomainEvent.OrderCompleted(id, now));
    }

    public void cancel(String reason) {
        if (status instanceof OrderStatus.Completed || status instanceof OrderStatus.Cancelled) {
            throw new DomainException("cannot cancel order in state " + status);
        }
        var now = Instant.now();
        status = new OrderStatus.Cancelled(now, reason);
        domainEvents.add(new OrderDomainEvent.OrderCancelled(id, reason, now));
    }

    public List<OrderDomainEvent> drainEvents() {
        var copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    public OrderId id() { return id; }
    public UserId buyerId() { return buyerId; }
    public List<OrderLine> lines() { return lines; }
    public Money totalAmount() { return totalAmount; }
    public OrderStatus status() { return status; }
    public long version() { return version; }

    private static Money sumOf(List<OrderLine> lines) {
        var first = lines.get(0).unitPrice();
        Money total = Money.zero(first.currency());
        for (var line : lines) {
            total = total.add(line.subtotal());
        }
        return total;
    }
}
