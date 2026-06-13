package com.tutorial.ecommerce.inventory.adapter.outbound.persistence;

import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "stocks", schema = "inventory")
class StockJpaEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    private int available;
    private int reserved;

    @Version
    private Long version;

    protected StockJpaEntity() {}

    static StockJpaEntity fromDomain(Stock s) {
        var e = new StockJpaEntity();
        e.productId = s.productId().value();
        e.available = s.available().value();
        e.reserved = s.reserved().value();
        e.version = s.version();
        return e;
    }

    void updateFrom(Stock s) {
        this.available = s.available().value();
        this.reserved = s.reserved().value();
    }

    Stock toDomain() {
        return Stock.rehydrate(
            new ProductId(productId),
            Quantity.of(available),
            Quantity.of(reserved),
            version == null ? 0 : version
        );
    }
}
