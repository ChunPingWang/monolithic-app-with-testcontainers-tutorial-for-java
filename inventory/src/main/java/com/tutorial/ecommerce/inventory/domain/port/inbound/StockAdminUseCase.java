package com.tutorial.ecommerce.inventory.domain.port.inbound;

import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;

public interface StockAdminUseCase {

    void seed(ProductId productId, Quantity initialAvailable);
}
