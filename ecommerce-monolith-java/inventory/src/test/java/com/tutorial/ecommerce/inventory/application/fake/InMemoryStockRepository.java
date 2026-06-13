package com.tutorial.ecommerce.inventory.application.fake;

import com.tutorial.ecommerce.inventory.domain.model.Reservation;
import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStockRepository implements StockWriteRepository {

    private final Map<ProductId, Stock> stocks = new ConcurrentHashMap<>();
    private final Map<OrderId, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public Stock save(Stock stock) { stocks.put(stock.productId(), stock); return stock; }

    @Override
    public Optional<Stock> findByProductId(ProductId id) { return Optional.ofNullable(stocks.get(id)); }

    @Override
    public void saveReservation(Reservation r) { reservations.put(r.orderId(), r); }

    @Override
    public Optional<Reservation> findReservation(OrderId orderId) { return Optional.ofNullable(reservations.get(orderId)); }

    @Override
    public void deleteReservation(OrderId orderId) { reservations.remove(orderId); }
}
