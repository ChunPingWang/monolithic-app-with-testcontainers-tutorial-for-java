package com.tutorial.ecommerce.inventory.domain.port.inbound;

import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;

import java.util.List;

public interface DeductInventoryUseCase {

    void deduct(DeductInventoryCommand command);

    record DeductInventoryCommand(OrderId orderId, PaymentId paymentId, List<Line> lines) {}

    record Line(ProductId productId, Quantity quantity) {}
}
