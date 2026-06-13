package com.tutorial.ecommerce.product.adapter.outbound.persistence;

import com.tutorial.ecommerce.product.domain.model.OrderLine;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class OrderLineJpaEntity implements Serializable {

    @Column(name = "line_index", nullable = false)
    private int lineIndex;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_amount", precision = 19, scale = 4)
    private BigDecimal unitAmount;

    @Column(name = "unit_currency", length = 3)
    private String unitCurrency;

    protected OrderLineJpaEntity() {}

    OrderLineJpaEntity(int lineIndex, OrderLine line) {
        this.lineIndex = lineIndex;
        this.productId = line.productId().value();
        this.quantity = line.quantity().value();
        this.unitAmount = line.unitPrice().amount();
        this.unitCurrency = line.unitPrice().currency().getCurrencyCode();
    }

    int lineIndex() { return lineIndex; }

    OrderLine toDomain() {
        return new OrderLine(
            new ProductId(productId),
            Quantity.of(quantity),
            new Money(unitAmount, Currency.getInstance(unitCurrency))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrderLineJpaEntity other)) return false;
        return lineIndex == other.lineIndex && Objects.equals(productId, other.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineIndex, productId);
    }
}
