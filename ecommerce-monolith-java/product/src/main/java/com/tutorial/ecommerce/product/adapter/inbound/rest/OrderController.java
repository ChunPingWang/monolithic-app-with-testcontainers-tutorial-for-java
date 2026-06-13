package com.tutorial.ecommerce.product.adapter.inbound.rest;

import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase.LineSpec;
import com.tutorial.ecommerce.product.domain.port.inbound.PlaceOrderUseCase.PlaceOrderCommand;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrder;

    public OrderController(PlaceOrderUseCase placeOrder) {
        this.placeOrder = placeOrder;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody PlaceOrderRequest req
    ) {
        var buyer = new UserId(jwt.getSubject());
        var lines = req.lines().stream()
            .map(l -> new LineSpec(
                new ProductId(l.productId()),
                Quantity.of(l.quantity()),
                Money.of(l.unitAmount().toPlainString(), l.unitCurrency())))
            .toList();

        OrderId id = placeOrder.handle(new PlaceOrderCommand(buyer, lines));
        return ResponseEntity.created(URI.create("/api/orders/" + id.value()))
            .body(new PlaceOrderResponse(id.value()));
    }

    public record PlaceOrderRequest(@NotEmpty List<@Valid LineRequest> lines) {}

    public record LineRequest(
        @NotNull UUID productId,
        @Positive int quantity,
        @NotNull BigDecimal unitAmount,
        @NotNull String unitCurrency
    ) {}

    public record PlaceOrderResponse(UUID orderId) {}
}
