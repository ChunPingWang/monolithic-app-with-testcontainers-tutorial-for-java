package com.tutorial.ecommerce.inventory.domain.port.outbound;

import com.tutorial.ecommerce.inventory.domain.model.Reservation;
import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

import java.util.Optional;

public interface StockWriteRepository {

    Stock save(Stock stock);

    Optional<Stock> findByProductId(ProductId id);

    void saveReservation(Reservation reservation);

    Optional<Reservation> findReservation(OrderId orderId);

    void deleteReservation(OrderId orderId);
}
