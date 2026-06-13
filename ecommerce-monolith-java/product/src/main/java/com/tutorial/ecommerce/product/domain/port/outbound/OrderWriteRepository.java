package com.tutorial.ecommerce.product.domain.port.outbound;

import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;

import java.util.Optional;

public interface OrderWriteRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);
}
