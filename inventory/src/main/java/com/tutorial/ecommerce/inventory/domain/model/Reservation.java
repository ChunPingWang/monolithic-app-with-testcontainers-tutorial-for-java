package com.tutorial.ecommerce.inventory.domain.model;

import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;

import java.time.Instant;

public record Reservation(OrderId orderId, ProductId productId, Quantity quantity, Instant reservedAt) {}
