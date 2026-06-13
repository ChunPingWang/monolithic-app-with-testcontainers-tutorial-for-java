package com.tutorial.ecommerce.payment.application;

import com.tutorial.ecommerce.payment.application.command.ProcessPaymentCommandHandler;
import com.tutorial.ecommerce.payment.application.fake.AlwaysDeclinedGateway;
import com.tutorial.ecommerce.payment.application.fake.AlwaysOkGateway;
import com.tutorial.ecommerce.payment.application.fake.InMemoryObjectStorage;
import com.tutorial.ecommerce.payment.application.fake.InMemoryPaymentRepository;
import com.tutorial.ecommerce.payment.domain.DomainException;
import com.tutorial.ecommerce.payment.domain.model.PaymentStatus;
import com.tutorial.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.tutorial.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase.ProcessPaymentCommand;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.event.PaymentCompletedEvent;
import com.tutorial.ecommerce.sharedkernel.event.PaymentFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessPaymentCommandHandlerTest {

    private InMemoryPaymentRepository repo;
    private InMemoryObjectStorage storage;
    private List<Object> events;
    private ApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPaymentRepository();
        storage = new InMemoryObjectStorage();
        events = new ArrayList<>();
        publisher = new ApplicationEventPublisher() {
            @Override public void publishEvent(Object e) { events.add(e); }
            @Override public void publishEvent(ApplicationEvent e) { events.add(e); }
        };
    }

    @Test
    void process_okGateway_savesReceiptAndPublishesCompleted() {
        var handler = new ProcessPaymentCommandHandler(repo, new AlwaysOkGateway(), storage, publisher);
        var orderId = OrderId.newId();
        var paymentId = handler.process(new ProcessPaymentCommand(orderId, Money.of("100.00", "TWD"), "k1"));

        var saved = repo.findById(paymentId).orElseThrow();
        assertThat(saved.status()).isInstanceOf(PaymentStatus.Completed.class);
        assertThat(events).hasSize(1).first().isInstanceOf(PaymentCompletedEvent.class);
        var receiptKey = ((PaymentStatus.Completed) saved.status()).receiptObjectKey();
        assertThat(storage.exists("payment-receipts", receiptKey)).isTrue();
    }

    @Test
    void process_declinedGateway_publishesFailedAndThrows() {
        var handler = new ProcessPaymentCommandHandler(repo, new AlwaysDeclinedGateway(), storage, publisher);

        assertThatThrownBy(() -> handler.process(
            new ProcessPaymentCommand(OrderId.newId(), Money.of("100.00", "TWD"), "k2")))
            .isInstanceOf(DomainException.class);

        assertThat(events).hasSize(1).first().isInstanceOf(PaymentFailedEvent.class);
    }

    @Test
    void process_sameIdempotencyKeyTwice_returnsSamePaymentId() {
        var handler = new ProcessPaymentCommandHandler(repo, new AlwaysOkGateway(), storage, publisher);
        var orderId = OrderId.newId();

        var first = handler.process(new ProcessPaymentCommand(orderId, Money.of("100.00", "TWD"), "idem-1"));
        var second = handler.process(new ProcessPaymentCommand(orderId, Money.of("100.00", "TWD"), "idem-1"));

        assertThat(second).isEqualTo(first);
        // 第二次不會發出事件
        assertThat(events).hasSize(1);
    }
}
