package com.tutorial.ecommerce.payment.adapter.outbound.gateway;

import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentGatewayPort;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.port.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 模擬第三方支付閘道 — 啟動時透過 SecretProvider 取出 API key,
 * 任何負金額視為 declined。真實版替換成 HTTP client。
 */
@Component
@ConditionalOnProperty(name = "ecommerce.adapter.gateway", havingValue = "secret-backed", matchIfMissing = true)
public class SecretBackedPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SecretBackedPaymentGatewayAdapter.class);

    private final SecretProvider secrets;
    private final String secretPath;
    private final String secretKey;

    public SecretBackedPaymentGatewayAdapter(
        SecretProvider secrets,
        @Value("${ecommerce.payment.gateway.secret-path:secret/payment-gateway}") String secretPath,
        @Value("${ecommerce.payment.gateway.secret-key:api-key}") String secretKey
    ) {
        this.secrets = secrets;
        this.secretPath = secretPath;
        this.secretKey = secretKey;
    }

    @Override
    public Result charge(String idempotencyKey, Money amount) {
        var apiKey = secrets.getString(secretPath, secretKey)
            .orElseThrow(() -> new IllegalStateException("payment gateway api key not configured"));
        log.debug("charging via gateway using apiKey of length={}", apiKey.length());

        if (amount.isNegative()) {
            return new Result.Declined("invalid amount");
        }
        return new Result.Ok(UUID.randomUUID().toString());
    }
}
