package com.tutorial.ecommerce.payment.domain.port.outbound;

import com.tutorial.ecommerce.sharedkernel.domain.Money;

/**
 * 模擬第三方支付閘道。Real adapter 從 Vault 拿 API key 呼叫遠端;
 * Test 用 InMemory 永遠成功或可控失敗。
 */
public interface PaymentGatewayPort {

    Result charge(String idempotencyKey, Money amount);

    sealed interface Result permits Result.Ok, Result.Declined {
        record Ok(String externalTxnId) implements Result {}
        record Declined(String reason) implements Result {}
    }
}
