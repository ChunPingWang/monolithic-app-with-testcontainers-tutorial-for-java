package com.tutorial.ecommerce.payment.application.fake;

import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentGatewayPort;
import com.tutorial.ecommerce.sharedkernel.domain.Money;

import java.util.UUID;

public class AlwaysOkGateway implements PaymentGatewayPort {
    @Override
    public Result charge(String idempotencyKey, Money amount) {
        return new Result.Ok("tx-" + UUID.randomUUID());
    }
}
