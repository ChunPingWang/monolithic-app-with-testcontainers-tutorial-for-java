package com.tutorial.ecommerce.e2e;

import com.tutorial.ecommerce.ECommerceApplication;
import com.tutorial.ecommerce.inventory.domain.port.inbound.StockAdminUseCase;
import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.PaymentStatus;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.LineRequest;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.PlaceOrderRequest;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.PlaceOrderResponse;
import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.product.domain.port.outbound.ProductWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Saga 補償:庫存不足 → InventoryDeductionFailedEvent
 *   → PaymentModule 訂閱 → 自動退款(Payment 狀態變 Refunded)
 */
@SpringBootTest(
    classes = ECommerceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = SharedContainersInitializer.class)
@ActiveProfiles("test-real")
class InventoryShortageSagaIT {

    @Autowired TestRestTemplate http;
    @Autowired ProductWriteRepository productRepo;
    @Autowired StockAdminUseCase stockAdmin;
    @Autowired PaymentWriteRepository payments;

    @BeforeAll
    static void provisionVaultSecret() throws Exception {
        SharedContainers.startAll();
        SharedContainers.VAULT.execInContainer("vault", "kv", "put",
            "secret/payment-gateway", "api-key=test-key-12345");
    }

    @Test
    void payment_refundedWhenInventoryDeductionFails() {
        var token = obtainJwt();
        var product = Product.create("Limited Edition", "out of stock", Money.of("1000.00", "TWD"));
        productRepo.save(product);
        stockAdmin.seed(product.id(), Quantity.of(0));   // 一開始就沒貨

        var req = new PlaceOrderRequest(List.of(
            new LineRequest(product.id().value(), 1, new BigDecimal("1000.00"), "TWD")));
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var resp = http.exchange("/api/orders", HttpMethod.POST,
            new HttpEntity<>(req, headers), PlaceOrderResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        var orderId = resp.getBody().orderId();
        var idempotencyKey = new IdempotencyKey("order-" + orderId);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            // Saga 流:支付完成 → 扣庫存失敗 → InventoryDeductionFailedEvent → 退款
            var refunded = payments.findByIdempotencyKey(idempotencyKey);
            assertThat(refunded).isPresent();
            assertThat(refunded.get().status()).isInstanceOf(PaymentStatus.Refunded.class);
        });
    }

    private String obtainJwt() {
        var url = SharedContainers.KEYCLOAK.getAuthServerUrl()
            + "/realms/ecommerce/protocol/openid-connect/token";
        var form = "grant_type=password&client_id=ecommerce-web&username=buyer01&password=secret&scope=openid";
        var headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        var rt = new TestRestTemplate();
        var resp = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
        return resp.getBody().get("access_token").toString();
    }
}
