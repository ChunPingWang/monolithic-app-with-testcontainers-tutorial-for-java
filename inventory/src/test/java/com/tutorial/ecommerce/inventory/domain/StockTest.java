package com.tutorial.ecommerce.inventory.domain;

import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    private final ProductId productId = ProductId.newId();

    @Test
    void reserve_decreasesAvailableIncreasesReserved() {
        var stock = Stock.create(productId, Quantity.of(10));

        stock.reserve(Quantity.of(3));

        assertThat(stock.available()).isEqualTo(Quantity.of(7));
        assertThat(stock.reserved()).isEqualTo(Quantity.of(3));
    }

    @Test
    void reserve_moreThanAvailable_throwsInsufficient() {
        var stock = Stock.create(productId, Quantity.of(2));

        assertThatThrownBy(() -> stock.reserve(Quantity.of(5)))
            .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void confirmReservation_drainsReserved() {
        var stock = Stock.create(productId, Quantity.of(10));
        stock.reserve(Quantity.of(3));

        stock.confirmReservation(Quantity.of(3));

        assertThat(stock.reserved()).isEqualTo(Quantity.of(0));
        assertThat(stock.available()).isEqualTo(Quantity.of(7));
    }

    @Test
    void releaseReservation_putsQuantityBack() {
        var stock = Stock.create(productId, Quantity.of(10));
        stock.reserve(Quantity.of(3));

        stock.releaseReservation(Quantity.of(3));

        assertThat(stock.available()).isEqualTo(Quantity.of(10));
        assertThat(stock.reserved()).isEqualTo(Quantity.of(0));
    }
}
