package com.tutorial.ecommerce.product.domain.model;

import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;

public record OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {

    public Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}
