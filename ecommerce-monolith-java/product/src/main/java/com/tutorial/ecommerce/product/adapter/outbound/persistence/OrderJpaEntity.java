package com.tutorial.ecommerce.product.adapter.outbound.persistence;

import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.model.OrderStatus;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Entity
@Table(name = "orders", schema = "product")
class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "total_currency", length = 3)
    private String totalCurrency;

    @Column(name = "status_kind", length = 20)
    private String statusKind;

    @Column(name = "status_at")
    private Instant statusAt;

    @Column(name = "status_payment_id")
    private UUID statusPaymentId;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "order_lines",
        schema = "product",
        joinColumns = @JoinColumn(name = "order_id")
    )
    @OrderColumn(name = "line_index")
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    @Version
    private Long version;

    protected OrderJpaEntity() {}

    static OrderJpaEntity fromDomain(Order o) {
        var e = new OrderJpaEntity();
        e.id = o.id().value();
        e.buyerId = o.buyerId().value();
        e.totalAmount = o.totalAmount().amount();
        e.totalCurrency = o.totalAmount().currency().getCurrencyCode();
        e.applyStatus(o.status());
        e.lines = IntStream.range(0, o.lines().size())
            .mapToObj(i -> new OrderLineJpaEntity(i, o.lines().get(i)))
            .toList();
        e.version = o.version();
        return e;
    }

    void updateFrom(Order o) {
        this.buyerId = o.buyerId().value();
        this.totalAmount = o.totalAmount().amount();
        this.totalCurrency = o.totalAmount().currency().getCurrencyCode();
        applyStatus(o.status());
        var rebuilt = new ArrayList<OrderLineJpaEntity>();
        for (int i = 0; i < o.lines().size(); i++) {
            rebuilt.add(new OrderLineJpaEntity(i, o.lines().get(i)));
        }
        this.lines = rebuilt;
    }

    private void applyStatus(OrderStatus status) {
        this.statusAt = status.at();
        this.statusPaymentId = null;
        this.statusReason = null;
        switch (status) {
            case OrderStatus.Created c -> this.statusKind = "CREATED";
            case OrderStatus.Paid p -> { this.statusKind = "PAID"; this.statusPaymentId = p.paymentId().value(); }
            case OrderStatus.Completed c -> this.statusKind = "COMPLETED";
            case OrderStatus.Cancelled c -> { this.statusKind = "CANCELLED"; this.statusReason = c.reason(); }
            case OrderStatus.Refunded r -> { this.statusKind = "REFUNDED"; this.statusReason = r.reason(); }
        }
    }

    Order toDomain() {
        var domainLines = lines.stream().map(OrderLineJpaEntity::toDomain).toList();
        var total = new Money(totalAmount, Currency.getInstance(totalCurrency));
        var status = switch (statusKind) {
            case "CREATED"   -> new OrderStatus.Created(statusAt);
            case "PAID"      -> new OrderStatus.Paid(statusAt, new PaymentId(statusPaymentId));
            case "COMPLETED" -> new OrderStatus.Completed(statusAt);
            case "CANCELLED" -> new OrderStatus.Cancelled(statusAt, statusReason);
            case "REFUNDED"  -> new OrderStatus.Refunded(statusAt, statusReason);
            default -> throw new IllegalStateException("unknown status kind: " + statusKind);
        };
        return Order.rehydrate(
            new OrderId(id),
            new UserId(buyerId),
            domainLines,
            total,
            status,
            version == null ? 0 : version
        );
    }

    UUID getId() { return id; }
}
