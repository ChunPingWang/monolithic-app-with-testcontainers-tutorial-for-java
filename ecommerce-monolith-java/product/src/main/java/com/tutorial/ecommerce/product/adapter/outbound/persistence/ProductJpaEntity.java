package com.tutorial.ecommerce.product.adapter.outbound.persistence;

import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "product")
class ProductJpaEntity {

    @Id
    private UUID id;
    private String name;
    private String description;

    @Column(name = "price_amount", precision = 19, scale = 4)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", length = 3)
    private String priceCurrency;

    @Column(name = "image_object_key")
    private String imageObjectKey;

    @Version
    private Long version;

    protected ProductJpaEntity() {}

    static ProductJpaEntity fromDomain(Product p) {
        var e = new ProductJpaEntity();
        e.id = p.id().value();
        e.name = p.name();
        e.description = p.description();
        e.priceAmount = p.price().amount();
        e.priceCurrency = p.price().currency().getCurrencyCode();
        e.imageObjectKey = p.imageObjectKey();
        e.version = p.version();
        return e;
    }

    Product toDomain() {
        return Product.rehydrate(
            new ProductId(id),
            name,
            description,
            new Money(priceAmount, Currency.getInstance(priceCurrency)),
            imageObjectKey,
            version == null ? 0 : version
        );
    }
}
