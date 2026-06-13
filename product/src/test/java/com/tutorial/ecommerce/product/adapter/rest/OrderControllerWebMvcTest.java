package com.tutorial.ecommerce.product.adapter.rest;

import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.LineRequest;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.PlaceOrderRequest;
import com.tutorial.ecommerce.product.adapter.inbound.rest.OrderController.PlaceOrderResponse;
import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase.PlaceOrderCommand;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Controller 行為單元測試 —
 * 1. 從 JWT 取出 buyerId 並轉成 LineSpec
 * 2. 回傳 201 Created + Location header
 * 3. 委派給 PlaceOrderUseCase
 *
 * Keycloak JWT 真實驗證留給 app 模組的 E2E。
 */
class OrderControllerWebMvcTest {

    private final PlaceOrderUseCase placeOrder = mock(PlaceOrderUseCase.class);
    private final OrderController controller = new OrderController(placeOrder);

    @Test
    void placeOrder_extractsBuyerFromJwt_andDelegatesToUseCase() {
        var orderId = OrderId.newId();
        when(placeOrder.handle(org.mockito.ArgumentMatchers.any())).thenReturn(orderId);
        var jwt = stubJwt("buyer-01");
        var productId = UUID.randomUUID();
        var req = new PlaceOrderRequest(List.of(
            new LineRequest(productId, 3, new BigDecimal("100.00"), "TWD")));

        ResponseEntity<PlaceOrderResponse> resp = controller.placeOrder(jwt, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().orderId()).isEqualTo(orderId.value());
        assertThat(resp.getHeaders().getLocation().getPath())
            .isEqualTo("/api/orders/" + orderId.value());

        var captor = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(placeOrder).handle(captor.capture());
        var cmd = captor.getValue();
        assertThat(cmd.buyerId().value()).isEqualTo("buyer-01");
        assertThat(cmd.lines()).hasSize(1);
        assertThat(cmd.lines().get(0).productId().value()).isEqualTo(productId);
        assertThat(cmd.lines().get(0).quantity().value()).isEqualTo(3);
    }

    private Jwt stubJwt(String subject) {
        return Jwt.withTokenValue("stub")
            .header("alg", "none")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("scope", "orders:write")
            .build();
    }
}
