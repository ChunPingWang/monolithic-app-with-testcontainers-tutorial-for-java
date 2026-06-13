/**
 * 商品模組(Product) — 訂單與商品 CRUD/CQRS。
 *
 * 對外發送的 Integration Event:
 *   - OrderCreatedEvent
 *
 * 監聽的 Integration Event:
 *   - PaymentFailedEvent  (補償:取消訂單)
 *   - InventoryDeductedEvent  (更新訂單為 COMPLETED)
 *   - InventoryDeductionFailedEvent  (補償:取消訂單並退款)
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Product"
)
package com.tutorial.ecommerce.product;
