package com.tutorial.ecommerce.inventory.domain.service;

import com.tutorial.ecommerce.inventory.domain.InsufficientStockException;
import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;

/**
 * 純函式 Domain Service。把 Stock 上的 reserve 邏輯與「批次預檢」分離,方便組合測試。
 */
public class StockAllocationService {

    public void reserveOrThrow(Stock stock, Quantity quantity) {
        if (!stock.available().isAtLeast(quantity)) {
            throw new InsufficientStockException(stock.productId(), quantity.value(), stock.available().value());
        }
        stock.reserve(quantity);
    }
}
