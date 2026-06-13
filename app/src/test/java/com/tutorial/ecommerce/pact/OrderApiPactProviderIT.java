package com.tutorial.ecommerce.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.tutorial.ecommerce.ECommerceApplication;
import com.tutorial.ecommerce.e2e.SharedContainersInitializer;
import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Consumer-Driven Contract — Provider 端驗證。
 *
 * 讀取 `contracts/pacts/*.json`(由 consumer 端產出的契約檔),
 * 用真實 HTTP 打進 OrderController,確認每個 interaction 的 request/response 都滿足。
 *
 * Consumer 端的測試在這個 repo 沒有(模擬 mobile-app 在另一個團隊維護),
 * 真實場景會由 Pact Broker 自動同步 pact 檔過來。
 *
 * 跑這個 IT 需要 Docker(走 SharedContainers 拉起完整 Spring Context)。
 */
@Provider("orders-api")
@PactFolder("contracts/pacts")
@SpringBootTest(
    classes = ECommerceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = SharedContainersInitializer.class)
@ActiveProfiles("test-real")
@Import(OrderApiPactProviderIT.PactSecurityOverride.class)
class OrderApiPactProviderIT {

    @LocalServerPort
    int port;

    @MockitoBean
    PlaceOrderUseCase placeOrder;

    @BeforeEach
    void setUpTarget(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyContract(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("buyer is authenticated and can place an order")
    void buyerCanPlaceOrder() {
        when(placeOrder.handle(any())).thenReturn(
            new OrderId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
        );
    }

    /**
     * Pact 測試用的 JWT decoder:接受任何 token,回傳一個固定的 Jwt,
     * 讓 pact 檔內可以寫 "Bearer pact-stub-token" 之類的固定字串。
     * 真實的 Keycloak 仍然在背景跑(SharedContainers 起的),
     * 只是 OAuth2 Resource Server 用我們覆寫的 decoder。
     */
    @Configuration
    static class PactSecurityOverride {
        @Bean @Primary
        JwtDecoder pactJwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("pact-buyer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("scope", "orders:write")
                .build();
        }
    }
}
