package com.tutorial.ecommerce.sharedkernel.event;

import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;

/**
 * 跨模組事件用的訂單明細 Snapshot。
 * 注意:這不是商品模組的 OrderLine Domain Entity,僅是事件酬載。
 */
public record OrderLineItem(ProductId productId, Quantity quantity, Money unitPrice) {}
