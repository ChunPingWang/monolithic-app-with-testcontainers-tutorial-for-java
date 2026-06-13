package com.tutorial.ecommerce.product.contract;

import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.model.OrderLine;
import com.tutorial.ecommerce.product.domain.model.OrderStatus;
import com.tutorial.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port Contract Test —
 * 同一組行為斷言,由 InMemory 與 JPA 兩個 adapter 各自繼承並提供 repository。
 * 確保 fake 與 real 行為一致(否則 unit test 綠 + 整合測試紅 = 騙人)。
 */
public abstract class OrderRepositoryContract {

    protected abstract OrderWriteRepository repository();

    @Test
    void saveThenFindById_roundTrip() {
        var order = Order.create(new UserId("buyer-01"),
            List.of(new OrderLine(ProductId.newId(), Quantity.of(2), Money.of("100.00", "TWD"))));

        repository().save(order);
        var loaded = repository().findById(order.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(order.id());
        assertThat(loaded.totalAmount()).isEqualTo(Money.of("200.00", "TWD"));
    }

    @Test
    void save_updatesStatusOnSubsequentSave() {
        var order = Order.create(new UserId("buyer-01"),
            List.of(new OrderLine(ProductId.newId(), Quantity.of(1), Money.of("50.00", "TWD"))));
        repository().save(order);

        var loaded = repository().findById(order.id()).orElseThrow();
        loaded.markPaid(PaymentId.newId());
        repository().save(loaded);

        var paid = repository().findById(order.id()).orElseThrow();
        assertThat(paid.status()).isInstanceOf(OrderStatus.Paid.class);
    }

    @Test
    void findById_missing_returnsEmpty() {
        assertThat(repository().findById(com.tutorial.ecommerce.sharedkernel.domain.OrderId.newId())).isEmpty();
    }
}
