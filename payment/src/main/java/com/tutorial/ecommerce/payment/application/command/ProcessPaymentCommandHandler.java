package com.tutorial.ecommerce.payment.application.command;

import com.tutorial.ecommerce.payment.domain.DomainException;
import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.Payment;
import com.tutorial.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentGatewayPort;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.event.PaymentCompletedEvent;
import com.tutorial.ecommerce.sharedkernel.event.PaymentFailedEvent;
import com.tutorial.ecommerce.sharedkernel.port.ObjectStoragePort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class ProcessPaymentCommandHandler implements ProcessPaymentUseCase {

    private static final String RECEIPT_BUCKET = "payment-receipts";

    private final PaymentWriteRepository payments;
    private final PaymentGatewayPort gateway;
    private final ObjectStoragePort storage;
    private final ApplicationEventPublisher events;

    public ProcessPaymentCommandHandler(PaymentWriteRepository payments, PaymentGatewayPort gateway,
                                        ObjectStoragePort storage, ApplicationEventPublisher events) {
        this.payments = payments;
        this.gateway = gateway;
        this.storage = storage;
        this.events = events;
    }

    @Override
    @Transactional
    public PaymentId process(ProcessPaymentCommand cmd) {
        var key = new IdempotencyKey(cmd.idempotencyKey());

        // 冪等保護:同一 key 直接回傳既有結果
        var existing = payments.findByIdempotencyKey(key);
        if (existing.isPresent()) return existing.get().id();

        var payment = Payment.initiate(cmd.orderId(), cmd.amount(), key);
        payment = payments.save(payment);

        var result = gateway.charge(cmd.idempotencyKey(), cmd.amount());
        switch (result) {
            case PaymentGatewayPort.Result.Ok ok -> {
                var receiptKey = "receipts/" + payment.id().value() + ".txt";
                var body = ("Receipt for " + payment.orderId().value() + " tx=" + ok.externalTxnId())
                    .getBytes(StandardCharsets.UTF_8);
                storage.put(RECEIPT_BUCKET, receiptKey, new ByteArrayInputStream(body), body.length, "text/plain");
                payment.markCompleted(receiptKey);
                payments.save(payment);
                events.publishEvent(new PaymentCompletedEvent(
                    payment.id(), payment.orderId(), payment.amount(), receiptKey, Instant.now()));
            }
            case PaymentGatewayPort.Result.Declined declined -> {
                payment.markFailed(declined.reason());
                payments.save(payment);
                events.publishEvent(new PaymentFailedEvent(
                    payment.id(), payment.orderId(), declined.reason(), Instant.now()));
                throw new DomainException("payment declined: " + declined.reason());
            }
        }
        return payment.id();
    }
}
