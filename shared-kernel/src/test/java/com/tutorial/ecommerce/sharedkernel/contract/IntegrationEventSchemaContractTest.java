package com.tutorial.ecommerce.sharedkernel.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.OrderId;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;
import com.tutorial.ecommerce.sharedkernel.event.InventoryDeductedEvent;
import com.tutorial.ecommerce.sharedkernel.event.InventoryDeductionFailedEvent;
import com.tutorial.ecommerce.sharedkernel.event.OrderCreatedEvent;
import com.tutorial.ecommerce.sharedkernel.event.OrderLineItem;
import com.tutorial.ecommerce.sharedkernel.event.PaymentCompletedEvent;
import com.tutorial.ecommerce.sharedkernel.event.PaymentFailedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Event Schema Contract Test.
 *
 * Integration Event 是跨 Bounded Context 的「契約」,一旦發布出去,
 * 任何欄位改名 / 刪除 / 型別變更都會默默砸壞另一個 context 的 listener。
 *
 * 這份測試把每個事件序列化成 JSON,跟「已公布的欄位路徑清單」對照。
 * 想新增欄位:更新清單;不該動到舊欄位的位置與名稱。
 * 想刪除欄位:你正在做 breaking change,先想清楚怎麼處理舊版 consumer。
 */
class IntegrationEventSchemaContractTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * 第一次跑這個契約測試就會發現:`Money.isNegative()` 與 `Quantity.isZero()` 這類
     * boolean 取值方法被 Jackson 當成 property 序列化成 `negative` / `zero` 欄位 —
     * 等於把「實作細節」公開到事件契約上,日後若刪除這些方法就是 breaking change。
     *
     * 真實專案應該在 Money / Quantity 上加 `@JsonIgnore` 或改方法命名;
     * 這份測試的意義就是「把所有真的被公開的東西列出來」,讓 PR 審查時看得到。
     */
    @Test
    void orderCreatedEvent_schema() throws Exception {
        var event = new OrderCreatedEvent(
            OrderId.newId(),
            new UserId("buyer-01"),
            List.of(new OrderLineItem(ProductId.newId(), Quantity.of(2), Money.of("100.00", "TWD"))),
            Money.of("200.00", "TWD"),
            Instant.parse("2026-01-01T00:00:00Z")
        );

        var paths = EventJsonFlattener.fieldPaths(mapper.readTree(mapper.writeValueAsString(event)));

        assertThat(paths).containsExactlyInAnyOrder(
            "orderId.value",
            "buyerId.value",
            "lines.[].productId.value",
            "lines.[].quantity.value",
            "lines.[].quantity.zero",          // 暴露的 isZero() — TODO: @JsonIgnore
            "lines.[].unitPrice.amount",
            "lines.[].unitPrice.currency",
            "lines.[].unitPrice.negative",     // 暴露的 isNegative() — TODO: @JsonIgnore
            "totalAmount.amount",
            "totalAmount.currency",
            "totalAmount.negative",            // 暴露的 isNegative() — TODO: @JsonIgnore
            "occurredAt"
        );
    }

    @Test
    void paymentCompletedEvent_schema() throws Exception {
        var event = new PaymentCompletedEvent(
            PaymentId.newId(),
            OrderId.newId(),
            Money.of("100.00", "TWD"),
            "receipts/r-001.pdf",
            Instant.parse("2026-01-01T00:00:00Z")
        );

        var paths = EventJsonFlattener.fieldPaths(mapper.readTree(mapper.writeValueAsString(event)));

        assertThat(paths).contains(
            "paymentId.value",
            "orderId.value",
            "paidAmount.amount",
            "receiptObjectKey",
            "occurredAt"
        );
    }

    @Test
    void paymentFailedEvent_schema() throws Exception {
        var event = new PaymentFailedEvent(
            PaymentId.newId(),
            OrderId.newId(),
            "card declined",
            Instant.parse("2026-01-01T00:00:00Z")
        );

        var paths = EventJsonFlattener.fieldPaths(mapper.readTree(mapper.writeValueAsString(event)));

        assertThat(paths).containsExactlyInAnyOrder(
            "paymentId.value",
            "orderId.value",
            "reason",
            "occurredAt"
        );
    }

    @Test
    void inventoryDeductedEvent_schema() throws Exception {
        var event = new InventoryDeductedEvent(
            OrderId.newId(),
            Instant.parse("2026-01-01T00:00:00Z")
        );

        var paths = EventJsonFlattener.fieldPaths(mapper.readTree(mapper.writeValueAsString(event)));

        assertThat(paths).containsExactlyInAnyOrder(
            "orderId.value",
            "occurredAt"
        );
    }

    @Test
    void inventoryDeductionFailedEvent_schema() throws Exception {
        var event = new InventoryDeductionFailedEvent(
            OrderId.newId(),
            PaymentId.newId(),
            "out of stock",
            Instant.parse("2026-01-01T00:00:00Z")
        );

        var paths = EventJsonFlattener.fieldPaths(mapper.readTree(mapper.writeValueAsString(event)));

        assertThat(paths).containsExactlyInAnyOrder(
            "orderId.value",
            "paymentId.value",
            "reason",
            "occurredAt"
        );
    }
}
