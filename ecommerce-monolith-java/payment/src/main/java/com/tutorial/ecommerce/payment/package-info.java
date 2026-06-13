/**
 * 支付模組(Payment) — 接收 OrderCreatedEvent 後執行扣款。
 *
 * 對外發送的 Integration Event:
 *   - PaymentCompletedEvent
 *   - PaymentFailedEvent
 *
 * 監聽的 Integration Event:
 *   - OrderCreatedEvent
 *   - InventoryDeductionFailedEvent  (補償:退款)
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Payment"
)
package com.tutorial.ecommerce.payment;
