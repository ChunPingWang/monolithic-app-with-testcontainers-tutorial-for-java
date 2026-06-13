package com.tutorial.ecommerce.product.domain.port.inbound;

import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;

import java.util.List;

public interface PlaceOrderUseCase {

    OrderId handle(PlaceOrderCommand command);

    record PlaceOrderCommand(UserId buyerId, List<LineSpec> lines) {}

    record LineSpec(ProductId productId, Quantity quantity, Money unitPrice) {}
}
