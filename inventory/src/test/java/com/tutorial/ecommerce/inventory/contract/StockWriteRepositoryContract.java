package com.tutorial.ecommerce.inventory.contract;

import com.tutorial.ecommerce.inventory.domain.model.Reservation;
import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port Contract Test — Inventory 模組的 StockWriteRepository 必須遵守的行為。
 * InMemory 與 JPA 兩個 adapter 各自繼承並提供 repository(),跑同樣的 assertion。
 */
public abstract class StockWriteRepositoryContract {

    protected abstract StockWriteRepository repository();

    @Test
    void saveStock_andLoadByProductId_roundTrip() {
        var productId = ProductId.newId();
        var stock = Stock.create(productId, Quantity.of(100));

        repository().save(stock);
        var loaded = repository().findByProductId(productId).orElseThrow();

        assertThat(loaded.productId()).isEqualTo(productId);
        assertThat(loaded.available()).isEqualTo(Quantity.of(100));
        assertThat(loaded.reserved()).isEqualTo(Quantity.of(0));
    }

    @Test
    void saveStock_updatesQuantitiesOnSubsequentSave() {
        var productId = ProductId.newId();
        var stock = Stock.create(productId, Quantity.of(10));
        repository().save(stock);

        var loaded = repository().findByProductId(productId).orElseThrow();
        loaded.reserve(Quantity.of(3));
        repository().save(loaded);

        var afterReserve = repository().findByProductId(productId).orElseThrow();
        assertThat(afterReserve.available()).isEqualTo(Quantity.of(7));
        assertThat(afterReserve.reserved()).isEqualTo(Quantity.of(3));
    }

    @Test
    void findByProductId_missing_returnsEmpty() {
        assertThat(repository().findByProductId(ProductId.newId())).isEmpty();
    }

    @Test
    void reservation_saveLoadAndDelete_roundTrip() {
        var productId = ProductId.newId();
        repository().save(Stock.create(productId, Quantity.of(100)));
        var orderId = OrderId.newId();
        var reservation = new Reservation(orderId, productId, Quantity.of(2), Instant.now());

        repository().saveReservation(reservation);
        var found = repository().findReservation(orderId).orElseThrow();
        assertThat(found.orderId()).isEqualTo(orderId);
        assertThat(found.quantity()).isEqualTo(Quantity.of(2));

        repository().deleteReservation(orderId);
        assertThat(repository().findReservation(orderId)).isEmpty();
    }

    @Test
    void findReservation_missing_returnsEmpty() {
        assertThat(repository().findReservation(OrderId.newId())).isEmpty();
    }
}
