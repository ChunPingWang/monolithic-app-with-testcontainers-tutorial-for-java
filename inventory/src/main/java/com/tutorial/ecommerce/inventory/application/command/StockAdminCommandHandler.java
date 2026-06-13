package com.tutorial.ecommerce.inventory.application.command;

import com.tutorial.ecommerce.inventory.domain.model.Stock;
import com.tutorial.ecommerce.inventory.domain.port.inbound.StockAdminUseCase;
import com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockAdminCommandHandler implements StockAdminUseCase {

    private final StockWriteRepository stocks;

    public StockAdminCommandHandler(StockWriteRepository stocks) {
        this.stocks = stocks;
    }

    @Override
    @Transactional
    public void seed(ProductId productId, Quantity initialAvailable) {
        var stock = stocks.findByProductId(productId)
            .orElseGet(() -> Stock.create(productId, initialAvailable));
        stocks.save(stock);
    }
}
