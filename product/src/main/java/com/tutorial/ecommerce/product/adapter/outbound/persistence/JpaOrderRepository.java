package com.tutorial.ecommerce.product.adapter.outbound.persistence;

import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaOrderRepository implements OrderWriteRepository {

    private final OrderJpaRepository delegate;

    public JpaOrderRepository(OrderJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Order save(Order order) {
        var entity = delegate.findById(order.id().value())
            .map(existing -> { existing.updateFrom(order); return existing; })
            .orElseGet(() -> OrderJpaEntity.fromDomain(order));
        return delegate.save(entity).toDomain();
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return delegate.findById(id.value()).map(OrderJpaEntity::toDomain);
    }
}
