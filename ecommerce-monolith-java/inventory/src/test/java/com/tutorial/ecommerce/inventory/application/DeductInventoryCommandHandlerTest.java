package com.tutorial.ecommerce.inventory.application;

import com.tutorial.ecommerce.inventory.application.command.DeductInventoryCommandHandler;
import com.tutorial.ecommerce.inventory.application.fake.InMemoryStockRepository;
import com.tutorial.ecommerce.inventory.application.fake.PassthroughLock;
import com.tutorial.ecommerce.inventory.domain.InsufficientStockException;
import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase.DeductInventoryCommand;
import com.tutorial.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase.Line;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.event.InventoryDeductedEvent;
import com.tutorial.ecommerce.sharedkernel.event.InventoryDeductionFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeductInventoryCommandHandlerTest {

    private InMemoryStockRepository repo;
    private List<Object> events;
    private DeductInventoryCommandHandler handler;

    @BeforeEach
    void setUp() {
        repo = new InMemoryStockRepository();
        events = new ArrayList<>();
        ApplicationEventPublisher publisher = new ApplicationEventPublisher() {
            @Override public void publishEvent(Object e) { events.add(e); }
            @Override public void publishEvent(ApplicationEvent e) { events.add(e); }
        };
        handler = new DeductInventoryCommandHandler(repo, new PassthroughLock(), publisher);
    }

    @Test
    void deduct_enoughStock_succeedsAndPublishesDeducted() {
        var productId = ProductId.newId();
        repo.save(Stock.create(productId, Quantity.of(10)));

        handler.deduct(new DeductInventoryCommand(
            OrderId.newId(), PaymentId.newId(),
            List.of(new Line(productId, Quantity.of(2)))));

        var stock = repo.findByProductId(productId).orElseThrow();
        assertThat(stock.available()).isEqualTo(Quantity.of(8));
        assertThat(events).hasSize(1).first().isInstanceOf(InventoryDeductedEvent.class);
    }

    @Test
    void deduct_insufficientStock_publishesFailedAndThrows() {
        var productId = ProductId.newId();
        repo.save(Stock.create(productId, Quantity.of(1)));

        var cmd = new DeductInventoryCommand(
            OrderId.newId(), PaymentId.newId(),
            List.of(new Line(productId, Quantity.of(5))));

        assertThatThrownBy(() -> handler.deduct(cmd))
            .isInstanceOf(InsufficientStockException.class);

        assertThat(events).hasSize(1).first().isInstanceOf(InventoryDeductionFailedEvent.class);
    }
}
