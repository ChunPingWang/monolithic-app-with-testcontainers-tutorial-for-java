package com.tutorial.ecommerce.payment.contract;

import com.tutorial.ecommerce.payment.application.fake.InMemoryPaymentRepository;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;

class InMemoryPaymentRepositoryContractTest extends PaymentRepositoryContract {

    private final InMemoryPaymentRepository repo = new InMemoryPaymentRepository();

    @Override
    protected PaymentWriteRepository repository() { return repo; }
}
