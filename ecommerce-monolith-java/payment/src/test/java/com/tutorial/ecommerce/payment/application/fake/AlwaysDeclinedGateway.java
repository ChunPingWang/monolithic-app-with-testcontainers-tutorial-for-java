package com.tutorial.ecommerce.payment.application.fake;

import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentGatewayPort;
import com.tutorial.ecommerce.sharedkernel.domain.Money;

public class AlwaysDeclinedGateway implements PaymentGatewayPort {
    @Override
    public Result charge(String idempotencyKey, Money amount) {
        return new Result.Declined("insufficient funds");
    }
}
