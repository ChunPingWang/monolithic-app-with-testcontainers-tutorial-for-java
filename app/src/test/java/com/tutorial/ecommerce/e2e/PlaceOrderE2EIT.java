package com.tutorial.ecommerce.e2e;

import com.tutorial.ecommerce.ECommerceApplication;
import com.tutorial.ecommerce.inventory.domain.port.inbound.StockAdminUseCase;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.LineRequest;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.PlaceOrderRequest;
import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.product.domain.port.outbound.ProductWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
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
 * 全鏈路 E2E:
 *   1. Keycloak 取 JWT
 *   2. POST /api/orders → OrderCreatedEvent
 *   3. PaymentModule 接收 → PaymentCompletedEvent
 *   4. InventoryModule 接收 → InventoryDeductedEvent
 *   5. 用 Awaitility 等待最終一致性收斂(庫存被扣)
 *
 * 跑這個測試需要 Docker。
 */
@SpringBootTest(
    classes = ECommerceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = SharedContainersInitializer.class)
@ActiveProfiles("test-real")
class PlaceOrderE2EIT {

    @Autowired TestRestTemplate http;
    @Autowired ProductWriteRepository productRepo;
    @Autowired StockAdminUseCase stockAdmin;
    @Autowired
    com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository inventoryRead;

    private static String accessToken;

    @BeforeAll
    static void provisionVaultSecret() throws Exception {
        SharedContainers.startAll();
        SharedContainers.VAULT.execInContainer("vault", "kv", "put",
            "secret/payment-gateway", "api-key=test-key-12345");
    }

    @Test
    void placeOrder_happyPath_endsWithInventoryDeducted() {
        accessToken = obtainJwt();
        var product = Product.create("iPhone 16", "flagship", Money.of("30000.00", "TWD"));
        productRepo.save(product);
        stockAdmin.seed(product.id(), Quantity.of(10));

        var req = new PlaceOrderRequest(List.of(
            new LineRequest(product.id().value(), 2, new BigDecimal("30000.00"), "TWD")));
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        var resp = http.exchange("/api/orders", HttpMethod.POST,
            new HttpEntity<>(req, headers), Map.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var stock = inventoryRead.findByProductId(product.id()).orElseThrow();
            assertThat(stock.available().value()).isEqualTo(8);
        });
    }

    private String obtainJwt() {
        var url = SharedContainers.KEYCLOAK.getAuthServerUrl()
            + "/realms/ecommerce/protocol/openid-connect/token";
        var form = "grant_type=password"
            + "&client_id=ecommerce-web"
            + "&username=buyer01"
            + "&password=secret"
            + "&scope=openid";
        var headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        var rt = new TestRestTemplate();
        var resp = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
        return resp.getBody().get("access_token").toString();
    }
}
