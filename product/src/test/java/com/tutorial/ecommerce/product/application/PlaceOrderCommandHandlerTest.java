package com.tutorial.ecommerce.product.application;

import com.tutorial.ecommerce.product.application.command.PlaceOrderCommandHandler;
import com.tutorial.ecommerce.product.application.fake.InMemoryOrderRepository;
import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;
import com.tutorial.ecommerce.sharedkernel.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderCommandHandlerTest {

    @Test
    void handle_persistsOrder_andPublishesIntegrationEvent() {
        var repo = new InMemoryOrderRepository();
        var captured = new ArrayList<Object>();
        ApplicationEventPublisher publisher = new ApplicationEventPublisher() {
            @Override public void publishEvent(Object event) { captured.add(event); }
            @Override public void publishEvent(ApplicationEvent event) { captured.add(event); }
        };
        var handler = new PlaceOrderCommandHandler(repo, publisher);

        var orderId = handler.handle(new PlaceOrderUseCase.PlaceOrderCommand(
            new UserId("buyer01"),
            List.of(new PlaceOrderUseCase.LineSpec(
                ProductId.newId(), Quantity.of(2), Money.of("100.00", "TWD")))
        ));

        assertThat(orderId).isNotNull();
        assertThat(repo.count()).isEqualTo(1);
        assertThat(captured).hasSize(1)
            .first().isInstanceOfSatisfying(OrderCreatedEvent.class, e -> {
                assertThat(e.orderId()).isEqualTo(orderId);
                assertThat(e.totalAmount()).isEqualTo(Money.of("200.00", "TWD"));
            });
    }
}
