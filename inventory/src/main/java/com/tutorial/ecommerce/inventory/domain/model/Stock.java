package com.tutorial.ecommerce.inventory.domain.model;

import com.tutorial.ecommerce.inventory.domain.DomainException;
import com.tutorial.ecommerce.inventory.domain.InsufficientStockException;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;

public class Stock {

    private final ProductId productId;
    private Quantity available;
    private Quantity reserved;
    private long version;

    private Stock(ProductId productId, Quantity available, Quantity reserved, long version) {
        this.productId = productId;
        this.available = available;
        this.reserved = reserved;
        this.version = version;
    }

    public static Stock create(ProductId productId, Quantity initialAvailable) {
        return new Stock(productId, initialAvailable, Quantity.of(0), 0L);
    }

    public static Stock rehydrate(ProductId productId, Quantity available, Quantity reserved, long version) {
        return new Stock(productId, available, reserved, version);
    }

    /** 扣減庫存:available 減去 qty,reserved 加上 qty。 */
    public void reserve(Quantity qty) {
        if (qty.isZero()) throw new DomainException("cannot reserve zero quantity");
        if (!available.isAtLeast(qty)) {
            throw new InsufficientStockException(productId, qty.value(), available.value());
        }
        this.available = available.subtract(qty);
        this.reserved = reserved.add(qty);
    }

    /** 確認預扣已實際出貨,reserved → 真正扣掉。 */
    public void confirmReservation(Quantity qty) {
        if (!reserved.isAtLeast(qty)) {
            throw new DomainException("cannot confirm beyond reserved");
        }
        this.reserved = reserved.subtract(qty);
    }

    /** 補償:取消預扣,把 reserved 退回 available。 */
    public void releaseReservation(Quantity qty) {
        if (!reserved.isAtLeast(qty)) {
            throw new DomainException("cannot release beyond reserved");
        }
        this.reserved = reserved.subtract(qty);
        this.available = available.add(qty);
    }

    public ProductId productId() { return productId; }
    public Quantity available() { return available; }
    public Quantity reserved() { return reserved; }
    public long version() { return version; }
}
