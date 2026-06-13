package com.tutorial.ecommerce.inventory.adapter.outbound.persistence;

import com.tutorial.ecommerce.inventory.domain.model.Reservation;
import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaStockRepository implements StockWriteRepository {

    private final StockJpaSpringDataRepository stocks;
    private final ReservationJpaSpringDataRepository reservations;

    public JpaStockRepository(StockJpaSpringDataRepository stocks, ReservationJpaSpringDataRepository reservations) {
        this.stocks = stocks;
        this.reservations = reservations;
    }

    @Override
    public Stock save(Stock stock) {
        var entity = stocks.findById(stock.productId().value())
            .map(existing -> { existing.updateFrom(stock); return existing; })
            .orElseGet(() -> StockJpaEntity.fromDomain(stock));
        return stocks.save(entity).toDomain();
    }

    @Override
    public Optional<Stock> findByProductId(ProductId id) {
        return stocks.findById(id.value()).map(StockJpaEntity::toDomain);
    }

    @Override
    public void saveReservation(Reservation reservation) {
        reservations.save(ReservationJpaEntity.fromDomain(reservation));
    }

    @Override
    public Optional<Reservation> findReservation(OrderId orderId) {
        return reservations.findById(orderId.value()).map(ReservationJpaEntity::toDomain);
    }

    @Override
    public void deleteReservation(OrderId orderId) {
        reservations.deleteById(orderId.value());
    }
}
