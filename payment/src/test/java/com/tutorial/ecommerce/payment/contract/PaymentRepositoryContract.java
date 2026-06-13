package com.tutorial.ecommerce.payment.contract;

import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.Payment;
import com.tutorial.ecommerce.payment.domain.model.PaymentStatus;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port Contract Test — Payment 模組的 PaymentWriteRepository 必須遵守的行為。
 *
 * 子類補上 repository(),InMemory 與 JPA 兩個 adapter 都會跑這些 assertion,
 * 確保「測試用的假 repository」跟「prod 用的真 repository」行為一致。
 */
public abstract class PaymentRepositoryContract {

    protected abstract PaymentWriteRepository repository();

    @Test
    void saveThenFindById_roundTrip() {
        var payment = Payment.initiate(OrderId.newId(), Money.of("100.00", "TWD"),
            new IdempotencyKey("idem-" + java.util.UUID.randomUUID()));

        repository().save(payment);
        var loaded = repository().findById(payment.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(payment.id());
        assertThat(loaded.amount()).isEqualTo(Money.of("100.00", "TWD"));
        assertThat(loaded.status()).isInstanceOf(PaymentStatus.Pending.class);
    }

    @Test
    void findByIdempotencyKey_returnsSavedPayment() {
        var key = new IdempotencyKey("order-12345");
        var payment = Payment.initiate(OrderId.newId(), Money.of("50.00", "TWD"), key);
        repository().save(payment);

        var found = repository().findByIdempotencyKey(key);

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(payment.id());
    }

    @Test
    void findByIdempotencyKey_missing_returnsEmpty() {
        assertThat(repository().findByIdempotencyKey(new IdempotencyKey("missing"))).isEmpty();
    }

    @Test
    void save_updatesStatusOnSubsequentSave() {
        var payment = Payment.initiate(OrderId.newId(), Money.of("100.00", "TWD"),
            new IdempotencyKey("idem-update-" + java.util.UUID.randomUUID()));
        repository().save(payment);

        var loaded = repository().findById(payment.id()).orElseThrow();
        loaded.markCompleted("receipts/r-001.pdf");
        repository().save(loaded);

        var completed = repository().findById(payment.id()).orElseThrow();
        assertThat(completed.status()).isInstanceOfSatisfying(PaymentStatus.Completed.class,
            c -> assertThat(c.receiptObjectKey()).isEqualTo("receipts/r-001.pdf"));
    }

    @Test
    void findById_missing_returnsEmpty() {
        assertThat(repository().findById(PaymentId.newId())).isEmpty();
    }
}
