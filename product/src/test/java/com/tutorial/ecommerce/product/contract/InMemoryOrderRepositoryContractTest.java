package com.tutorial.ecommerce.product.contract;

import com.tutorial.ecommerce.product.application.fake.InMemoryOrderRepository;
import com.tutorial.ecommerce.product.domain.port.outbound.OrderWriteRepository;

class InMemoryOrderRepositoryContractTest extends OrderRepositoryContract {

    private final InMemoryOrderRepository repo = new InMemoryOrderRepository();

    @Override
    protected OrderWriteRepository repository() { return repo; }
}
