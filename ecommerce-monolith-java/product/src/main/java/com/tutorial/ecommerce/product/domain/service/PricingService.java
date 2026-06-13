package com.tutorial.ecommerce.product.domain.service;

import com.tutorial.ecommerce.product.domain.DomainException;
import com.tutorial.ecommerce.product.domain.model.OrderLine;
import com.tutorial.ecommerce.sharedkernel.domain.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain Service — 跨 Aggregate 的價格計算邏輯。
 * 純函式,無狀態,適合放在 domain.service。
 */
public class PricingService {

    public Money totalOf(List<OrderLine> lines) {
        if (lines.isEmpty()) throw new DomainException("cannot total empty lines");
        var currency = lines.get(0).unitPrice().currency();
        Money total = Money.zero(currency);
        for (var line : lines) {
            total = total.add(line.subtotal());
        }
        return total;
    }

    /** 整單折扣(例:9 折 = 0.9)。 */
    public Money applyDiscount(Money total, BigDecimal multiplier) {
        if (multiplier.signum() < 0) throw new DomainException("discount multiplier must be >= 0");
        BigDecimal scaled = total.amount().multiply(multiplier)
            .setScale(total.currency().getDefaultFractionDigits(), java.math.RoundingMode.HALF_UP);
        return new Money(scaled, total.currency());
    }
}
