package com.tutorial.ecommerce.bdd;

import com.tutorial.ecommerce.inventory.domain.port.inbound.StockAdminUseCase;
import com.tutorial.ecommerce.inventory.domain.port.outbound.StockWriteRepository;
import com.tutorial.ecommerce.payment.domain.model.IdempotencyKey;
import com.tutorial.ecommerce.payment.domain.model.PaymentStatus;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.LineRequest;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.PlaceOrderRequest;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.PlaceOrderResponse;
import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.model.OrderStatus;
import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.tutorial.ecommerce.product.domain.port.outbound.ProductWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.port.ObjectStoragePort;
import io.cucumber.java.zh_tw.假設;
import io.cucumber.java.zh_tw.並且;
import io.cucumber.java.zh_tw.當;
import io.cucumber.java.zh_tw.那麼;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class PlaceOrderSteps {

    @Autowired TestRestTemplate http;
    @Autowired ProductWriteRepository products;
    @Autowired OrderWriteRepository orders;
    @Autowired StockAdminUseCase stockAdmin;
    @Autowired StockWriteRepository stocks;
    @Autowired PaymentWriteRepository payments;
    @Autowired ObjectStoragePort storage;

    private final Map<String, Product> productsByName = new HashMap<>();
    private String jwt;
    private OrderId lastOrderId;

    @假設("商品 {string} 庫存為 {int}")
    public void seedProduct(String name, int qty) {
        var product = Product.create(name, "BDD scenario product", Money.of("1000.00", "TWD"));
        products.save(product);
        productsByName.put(name, product);
        stockAdmin.seed(product.id(), Quantity.of(qty));
    }

    @假設("使用者 {string} 已通過認證")
    public void authenticate(String username) {
        this.jwt = obtainJwt(username);
    }

    @並且("使用者 {string} 已通過認證")
    public void authenticateAnd(String username) {
        authenticate(username);
    }

    @當("使用者下單購買 {int} 件 {string}")
    public void placeOrder(int qty, String productName) {
        var product = productsByName.get(productName);
        var req = new PlaceOrderRequest(List.of(
            new LineRequest(product.id().value(), qty, new BigDecimal("1000.00"), "TWD")));
        var headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        var resp = http.exchange("/api/orders", HttpMethod.POST,
            new HttpEntity<>(req, headers), PlaceOrderResponse.class);
        lastOrderId = new OrderId(resp.getBody().orderId());
    }

    @並且("支付成功")
    public void paymentSucceeds() {
        // 由事件鏈自動觸發,此步驟僅作斷言入口;真實支付路徑在 ProcessPaymentCommandHandler 處理
    }

    @那麼("庫存應減少為 {int}")
    public void inventoryShouldBe(int expected) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var stock = stocks.findByProductId(productsByName.values().iterator().next().id()).orElseThrow();
            assertThat(stock.available().value()).isEqualTo(expected);
        });
    }

    @並且("訂單狀態應為 {string}")
    public void orderStatusShouldBe(String expected) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var loaded = orders.findById(lastOrderId).orElseThrow();
            assertThat(orderStatusName(loaded.status())).isEqualTo(expected);
        });
    }

    @並且("支付憑證應存在於 MinIO")
    public void receiptShouldExist() {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var payment = payments.findByIdempotencyKey(
                new IdempotencyKey("order-" + lastOrderId.value())).orElseThrow();
            var key = ((PaymentStatus.Completed) payment.status()).receiptObjectKey();
            assertThat(storage.exists("payment-receipts", key)).isTrue();
        });
    }

    @那麼("應觸發退款流程")
    public void refundShouldBeTriggered() {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var payment = payments.findByIdempotencyKey(
                new IdempotencyKey("order-" + lastOrderId.value())).orElseThrow();
            assertThat(payment.status()).isInstanceOf(PaymentStatus.Refunded.class);
        });
    }

    @並且("支付狀態應為 {string}")
    public void paymentStatusShouldBe(String expected) {
        var payment = payments.findByIdempotencyKey(
            new IdempotencyKey("order-" + lastOrderId.value())).orElseThrow();
        assertThat(paymentStatusName(payment.status())).isEqualTo(expected);
    }

    private static String orderStatusName(OrderStatus s) {
        return switch (s) {
            case OrderStatus.Created c -> "CREATED";
            case OrderStatus.Paid p -> "PAID";
            case OrderStatus.Completed c -> "COMPLETED";
            case OrderStatus.Cancelled c -> "CANCELLED";
            case OrderStatus.Refunded r -> "REFUNDED";
        };
    }

    private static String paymentStatusName(PaymentStatus s) {
        return switch (s) {
            case PaymentStatus.Pending p -> "PENDING";
            case PaymentStatus.Completed c -> "COMPLETED";
            case PaymentStatus.Failed f -> "FAILED";
            case PaymentStatus.Refunded r -> "REFUNDED";
        };
    }

    private String obtainJwt(String username) {
        var url = com.tutorial.ecommerce.e2e.SharedContainers.KEYCLOAK.getAuthServerUrl()
            + "/realms/ecommerce/protocol/openid-connect/token";
        var form = "grant_type=password&client_id=ecommerce-web&username=" + username
            + "&password=secret&scope=openid";
        var headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        var rt = new TestRestTemplate();
        var resp = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
        return resp.getBody().get("access_token").toString();
    }
}
