/**
 * 庫存模組(Inventory) — 接收 PaymentCompletedEvent 後扣庫存。
 *
 * 對外發送的 Integration Event:
 *   - InventoryDeductedEvent
 *   - InventoryDeductionFailedEvent
 *
 * 監聽的 Integration Event:
 *   - PaymentCompletedEvent
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Inventory"
)
package com.tutorial.ecommerce.inventory;
