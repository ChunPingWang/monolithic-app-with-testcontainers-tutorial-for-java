package com.tutorial.ecommerce.product.application.fake;

import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderWriteRepository {

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    public int count() { return store.size(); }
}
