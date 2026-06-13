package com.tutorial.ecommerce.inventory.adapter.outbound.persistence;

import com.tutorial.ecommerce.inventory.domain.model.Reservation;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations", schema = "inventory")
class ReservationJpaEntity {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    private int quantity;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    protected ReservationJpaEntity() {}

    static ReservationJpaEntity fromDomain(Reservation r) {
        var e = new ReservationJpaEntity();
        e.orderId = r.orderId().value();
        e.productId = r.productId().value();
        e.quantity = r.quantity().value();
        e.reservedAt = r.reservedAt();
        return e;
    }

    Reservation toDomain() {
        return new Reservation(
            new OrderId(orderId),
            new ProductId(productId),
            Quantity.of(quantity),
            reservedAt
        );
    }
}
