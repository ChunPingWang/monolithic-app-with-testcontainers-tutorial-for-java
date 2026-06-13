package com.tutorial.ecommerce.inventory.contract;

import com.tutorial.ecommerce.inventory.application.fake.InMemoryStockRepository;
import com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository;

class InMemoryStockWriteRepositoryContractTest extends StockWriteRepositoryContract {

    private final InMemoryStockRepository repo = new InMemoryStockRepository();

    @Override
    protected StockWriteRepository repository() { return repo; }
}
