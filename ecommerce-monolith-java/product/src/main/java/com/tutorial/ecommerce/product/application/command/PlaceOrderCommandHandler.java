package com.tutorial.ecommerce.product.application.command;

import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.model.OrderLine;
import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.tutorial.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.event.OrderCreatedEvent;
import com.tutorial.ecommerce.sharedkernel.event.OrderLineItem;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PlaceOrderCommandHandler implements PlaceOrderUseCase {

    private final OrderWriteRepository repository;
    private final ApplicationEventPublisher events;

    public PlaceOrderCommandHandler(OrderWriteRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Override
    @Transactional
    public OrderId handle(PlaceOrderCommand command) {
        var lines = command.lines().stream()
            .map(l -> new OrderLine(l.productId(), l.quantity(), l.unitPrice()))
            .toList();

        var order = Order.create(command.buyerId(), lines);
        var saved = repository.save(order);
        saved.drainEvents();  // 模組內部 domain event 已透過 save 持久化記錄,清空即可

        var eventLines = lines.stream()
            .map(l -> new OrderLineItem(l.productId(), l.quantity(), l.unitPrice()))
            .toList();
        events.publishEvent(new OrderCreatedEvent(
            saved.id(), saved.buyerId(), eventLines, saved.totalAmount(), Instant.now()
        ));
        return saved.id();
    }
}
