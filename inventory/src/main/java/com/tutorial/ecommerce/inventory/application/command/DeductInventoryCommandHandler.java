package com.tutorial.ecommerce.inventory.application.command;

import com.tutorial.ecommerce.inventory.domain.InsufficientStockException;
import com.tutorial.ecommerce.inventory.domain.model.Reservation;
import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase;
import com.tutorial.ecommerce.inventory.domain.port.outbound.DistributedLockPort;
import com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository;
import com.tutorial.ecommerce.inventory.domain.service.StockAllocationService;
import com.tutorial.ecommerce.sharedkernel.event.InventoryDeductedEvent;
import com.tutorial.ecommerce.sharedkernel.event.InventoryDeductionFailedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class DeductInventoryCommandHandler implements DeductInventoryUseCase {

    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final StockWriteRepository stocks;
    private final DistributedLockPort lock;
    private final ApplicationEventPublisher events;
    private final StockAllocationService allocation = new StockAllocationService();

    public DeductInventoryCommandHandler(StockWriteRepository stocks, DistributedLockPort lock,
                                         ApplicationEventPublisher events) {
        this.stocks = stocks;
        this.lock = lock;
        this.events = events;
    }

    @Override
    @Transactional
    public void deduct(DeductInventoryCommand cmd) {
        try {
            for (var line : cmd.lines()) {
                lock.withLock("stock:" + line.productId().value(), LOCK_TTL, () -> {
                    var stock = stocks.findByProductId(line.productId())
                        .orElseThrow(() -> new InsufficientStockException(line.productId(), line.quantity().value(), 0));
                    allocation.reserveOrThrow(stock, line.quantity());
                    stocks.save(stock);
                    stocks.saveReservation(new Reservation(
                        cmd.orderId(), line.productId(), line.quantity(), Instant.now()));
                    return null;
                });
            }
            events.publishEvent(new InventoryDeductedEvent(cmd.orderId(), Instant.now()));
        } catch (InsufficientStockException e) {
            rollbackReservations(cmd);
            events.publishEvent(new InventoryDeductionFailedEvent(
                cmd.orderId(), cmd.paymentId(), e.getMessage(), Instant.now()));
            throw e;
        }
    }

    private void rollbackReservations(DeductInventoryCommand cmd) {
        var reservation = stocks.findReservation(cmd.orderId());
        if (reservation.isEmpty()) return;
        var r = reservation.get();
        stocks.findByProductId(r.productId()).ifPresent(stock -> {
            stock.releaseReservation(r.quantity());
            stocks.save(stock);
        });
        stocks.deleteReservation(cmd.orderId());
    }
}
