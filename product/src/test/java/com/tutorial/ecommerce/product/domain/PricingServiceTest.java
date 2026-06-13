package com.tutorial.ecommerce.product.domain;

import com.tutorial.ecommerce.product.domain.model.OrderLine;
import com.tutorial.ecommerce.product.domain.service.PricingService;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricing = new PricingService();

    @Test
    void totalOf_sumsLineSubtotals() {
        var a = new OrderLine(ProductId.newId(), Quantity.of(2), Money.of("100.00", "TWD"));
        var b = new OrderLine(ProductId.newId(), Quantity.of(1), Money.of("50.00", "TWD"));

        assertThat(pricing.totalOf(List.of(a, b))).isEqualTo(Money.of("250.00", "TWD"));
    }

    @Test
    void applyDiscount_roundsToCurrencyScale() {
        var total = Money.of("1000.00", "TWD");

        var discounted = pricing.applyDiscount(total, new BigDecimal("0.9"));

        assertThat(discounted).isEqualTo(Money.of("900.00", "TWD"));
    }

    @Test
    void applyDiscount_jpyHasZeroFractionDigits() {
        var total = Money.of("1000", "JPY");

        var discounted = pricing.applyDiscount(total, new BigDecimal("0.9"));

        assertThat(discounted).isEqualTo(Money.of("900", "JPY"));
    }
}
