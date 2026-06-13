# Testcontainers 電商模組化單體應用教程（Java 版）

## Spring Boot 4 + Spring ApplicationEventPublisher

---

## 一、架構轉型：微服務 → 模組化單體

### 1.1 什麼改變了

```
微服務版                              模組化單體版
─────────                            ──────────────
3 個獨立部署單元                      1 個部署單元，3 個邏輯模組
Kafka 跨服務事件流                    Spring ApplicationEventPublisher
每服務各自一個 DB                     共用 DB，Schema 隔離
Keycloak JWT 驗證每個服務             單一入口驗證
各服務獨立啟動 / 獨立掛               同生共死
```

### 1.2 什麼沒變

```
DDD Domain Model                     ✅ 保持不變
CQRS (Command / Query 分離)          ✅ 保持不變
Hexagonal Architecture               ✅ 保持不變
Outbound Port / Adapter 可切換        ✅ 保持不變
SOLID 原則                           ✅ 保持不變
Testcontainers 驗證真實基礎設施       ✅ 保持不變
```

### 1.3 容器精簡對照

| 元件             | 微服務版       | 單體版         | 原因                               |
|------------------|---------------|---------------|-------------------------------------|
| PostgreSQL       | × 3 (各服務)  | × 1 (Schema隔離)| 同一進程，共用連線池                 |
| Kafka            | ✅            | ❌ 移除        | 改用 Spring ApplicationEventPublisher |
| GCP Pub/Sub      | ✅            | ❌ 移除        | 外部通知降級為進程內事件              |
| Redis            | ✅            | ✅             | 快取 + 分散式鎖（部署多實例時仍需要）  |
| Elasticsearch    | ✅            | ✅             | 商品全文檢索                         |
| Keycloak         | ✅            | ✅             | 認證授權                             |
| MinIO            | ✅            | ✅             | 商品圖片 + 支付憑證                   |
| Vault            | ✅            | ✅             | 密鑰管理                             |

**Testcontainers 從 8 個減為 6 個（移除 Kafka + GCP Pub/Sub Emulator）**

---

### 1.4 架構圖

```
┌─────────────────────────────────────────────────────────────────┐
│                    單體應用 (Single Deployable)                   │
│                                                                   │
│  ┌─────────────┐                  ┌─────────────┐                │
│  │  商品模組    │  OrderCreated    │  支付模組    │                │
│  │  Product     │ ───────────→    │  Payment     │                │
│  │             │  Event           │             │                │
│  │  Domain     │  InventoryDe-    │  Domain     │                │
│  │  App        │ ←───────────     │  App        │                │
│  │  Adapter    │  ductedEvent     │  Adapter    │                │
│  └──────┬──────┘                  └──────┬──────┘                │
│         │          PaymentCom-           │                        │
│         │         pletedEvent           │                        │
│         │              │                │                        │
│         │    ┌─────────▼─────────┐      │                        │
│         │    │   庫存模組        │      │                        │
│         │    │   Inventory       │      │                        │
│         │    │                   │      │                        │
│         │    │   Domain          │      │                        │
│         │    │   App             │      │                        │
│         │    │   Adapter         │      │                        │
│         │    └─────────┬─────────┘      │                        │
│         │              │                │                        │
│  ┌──────▼──────────────▼────────────────▼──────┐                │
│  │              共用基礎設施                     │                │
│  │  Keycloak Auth │ Vault Secrets │ Config     │                │
│  └────────────────────────────────────────────┘                │
│                                                                   │
│         Event Bus: Spring ApplicationEventPublisher               │
│         Module Boundary: Spring Modulith                          │
└────────┬──────────┬──────────┬──────────┬──────────┬─────────────┘
         │          │          │          │          │
    PostgreSQL    Redis   Elasticsearch  MinIO     Vault
    (3 Schema)
```

### 1.5 模組間通訊規則

```
✅ 允許：透過 ApplicationEventPublisher 發送 Integration Event
✅ 允許：透過 Shared Kernel 的 Value Object 傳遞資料
❌ 禁止：模組 A 直接 import 模組 B 的 Domain Model
❌ 禁止：模組 A 直接存取模組 B 的 DB Table / Schema
❌ 禁止：跨模組 DB Transaction（為未來拆分保留彈性）
```

---

## 二、技術棧

| 類別             | 技術選型                                                |
|------------------|---------------------------------------------------------|
| Language         | Java 21 (Record, Sealed Interface, Pattern Matching)    |
| Framework        | Spring Boot 4.x + Spring Framework 7                    |
| Module System    | Spring Modulith (模組邊界驗證 + 事件日誌)                |
| Build            | Gradle 8.x (Kotlin DSL), 單一 multi-module project     |
| Event Bus        | Spring ApplicationEventPublisher                        |
| CQRS             | 手動 CommandHandler / QueryHandler                      |
| ORM              | Spring Data JPA / Hibernate                             |
| DB Migration     | Flyway 10 (per-schema migration)                        |
| Test             | JUnit 5, Testcontainers 1.20+, Awaitility, ArchUnit    |
| BDD              | Cucumber 7.x                                            |

### Spring Modulith 的角色

Spring Modulith 是 Spring 官方的模組化單體支援框架，提供：
- `@ApplicationModule` 定義模組邊界
- `ApplicationModuleTest` 驗證模組間依賴合規
- `EventPublication` 事件發佈日誌（確保事件不丟失，at-least-once delivery）
- 與 `ApplicationEventPublisher` 無縫整合

---

## 三、交易邊界設計

模組化單體最容易犯的錯：因為在同一進程就把所有模組包進同一個 DB Transaction。
這會導致未來拆分微服務時交易邊界全部要重寫。

### 策略：每模組獨立交易 + 最終一致性

```
[商品模組]                    [支付模組]                  [庫存模組]
    │                            │                          │
    │ TX-1: 建立訂單              │                          │
    │ COMMIT                     │                          │
    │                            │                          │
    │──→ OrderCreatedEvent ──→  │                          │
    │   (AFTER_COMMIT 才發送)     │ TX-2: 建立支付記錄        │
    │                            │ COMMIT                   │
    │                            │                          │
    │                            │──→ PaymentCompletedEvent →│
    │                            │                          │ TX-3: 扣庫存
    │                            │                          │ COMMIT
    │                            │                          │
    │←── InventoryDeductedEvent ←──────────────────────────│
    │ TX-4: 更新訂單狀態          │                          │
    │ COMMIT                     │                          │
```

每個事件觸發獨立交易，失敗時透過補償事件回滾。
這個設計讓未來拆分微服務時，只需把進程內事件換成 Kafka，交易邊界不需改動。

---

## 四、業務流程

```
使用者下單
    │
    ▼
[商品模組] 建立訂單 (寫入 product schema)
    │ applicationEventPublisher.publishEvent(OrderCreatedEvent)
    │ → @TransactionalEventListener(phase = AFTER_COMMIT)
    ▼
[支付模組] EventListener 接收
    │ 執行扣款 (寫入 payment schema)
    │ Vault 取得第三方支付 API Key
    │ MinIO 上傳支付憑證
    │ publishEvent(PaymentCompletedEvent)
    ▼
[庫存模組] EventListener 接收
    │ Redis 分散式鎖 → 扣庫存 (寫入 inventory schema)
    │ publishEvent(InventoryDeductedEvent)
    ▼
[商品模組] EventListener 接收
    │ 更新訂單狀態為 COMPLETED
    │ 更新 Elasticsearch 索引
    │ 清除 Redis 快取
    ▼
流程完成
```

---

## 五、Domain Model（Java 21 語言特性）

```java
// ── Value Object ─────────────────────────────────
public record OrderId(UUID value) {}
public record Money(BigDecimal amount, Currency currency) {}
public record OrderLine(ProductId productId, int quantity, Money unitPrice) {}

// ── 狀態：Sealed Interface + Pattern Matching ────
public sealed interface OrderStatus
    permits OrderStatus.Created, OrderStatus.Paid,
            OrderStatus.Completed, OrderStatus.Refunded {

    record Created(Instant at) implements OrderStatus {}
    record Paid(Instant at, PaymentId paymentId) implements OrderStatus {}
    record Completed(Instant at) implements OrderStatus {}
    record Refunded(Instant at, String reason) implements OrderStatus {}
}

// ── Domain Event：Sealed Interface ───────────────
public sealed interface OrderEvent
    permits OrderEvent.OrderCreated, OrderEvent.OrderPaid,
            OrderEvent.OrderCancelled {

    record OrderCreated(OrderId id, List<OrderLine> lines, Money total)
        implements OrderEvent {}
    record OrderPaid(OrderId id, PaymentId paymentId)
        implements OrderEvent {}
    record OrderCancelled(OrderId id, String reason)
        implements OrderEvent {}
}

// ── Aggregate Root ───────────────────────────────
public class Order {
    private OrderId id;
    private OrderStatus status;
    private List<OrderLine> lines;
    private Money totalAmount;
    private final List<OrderEvent> domainEvents = new ArrayList<>();

    public static Order create(List<OrderLine> lines) {
        var order = new Order();
        order.id = new OrderId(UUID.randomUUID());
        order.lines = List.copyOf(lines);
        order.totalAmount = calculateTotal(lines);
        order.status = new OrderStatus.Created(Instant.now());
        order.domainEvents.add(
            new OrderEvent.OrderCreated(order.id, order.lines, order.totalAmount)
        );
        return order;
    }

    public void markPaid(PaymentId paymentId) {
        if (!(status instanceof OrderStatus.Created))
            throw new DomainException("Only CREATED orders can be paid.");
        status = new OrderStatus.Paid(Instant.now(), paymentId);
        domainEvents.add(new OrderEvent.OrderPaid(id, paymentId));
    }
}
```

---

## 六、事件機制詳解

### 6.1 發送端（商品模組 — Application Service）

```java
@Service
@RequiredArgsConstructor
public class PlaceOrderCommandHandler {
    private final OrderWriteRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderId handle(PlaceOrderCommand command) {
        Order order = Order.create(command.lines());
        repository.save(order);
        return order.getId();
        // Domain Events 由 Spring Data @DomainEvents 自動收集發送
        // 或在 AFTER_COMMIT 手動發送 Integration Event
    }
}
```

### 6.2 Spring Data AbstractAggregateRoot 自動收集

```java
// 方式一：繼承 AbstractAggregateRoot
// Spring Data save() 時自動透過 ApplicationEventPublisher 發送

// 方式二：@DomainEvents + @AfterDomainEventPublication
@DomainEvents
Collection<Object> domainEvents() { return List.copyOf(domainEvents); }

@AfterDomainEventPublication
void clearEvents() { domainEvents.clear(); }
```

### 6.3 接收端（支付模組 — Event Listener）

```java
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {
    private final ProcessPaymentUseCase useCase;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        useCase.process(new ProcessPaymentCommand(
            event.orderId(), event.totalAmount()
        ));
    }
}
```

### 6.4 Spring Modulith Event Publication 保障

```java
// Spring Modulith 自動將事件寫入 event_publication 表
// 欄位：id, listener_id, event_type, serialized_event, completion_date
// 如果 Listener 處理失敗 → completion_date 為 null → 排程重新發送
// 確保 at-least-once delivery（即使進程內也不丟事件）

// application.yml
spring:
  modulith:
    events:
      republish-outstanding-events-on-restart: true
      jdbc:
        schema-initialization:
          enabled: true
```

---

## 七、Schema 隔離策略

```sql
-- 單一 PostgreSQL，三個 Schema
CREATE SCHEMA product;
CREATE SCHEMA payment;
CREATE SCHEMA inventory;
```

```
-- Flyway per-schema migration 結構
src/main/resources/
├── db/migration/product/
│   ├── V001__create_products.sql
│   └── V002__create_orders.sql
├── db/migration/payment/
│   ├── V001__create_payments.sql
│   └── V002__create_idempotency_keys.sql
└── db/migration/inventory/
    ├── V001__create_stocks.sql
    └── V002__create_reservations.sql
```

```yaml
# application.yml — 單一 DataSource
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce
  flyway:
    schemas: product, payment, inventory
  jpa:
    properties:
      hibernate:
        default_schema: product
```

```java
// 各模組 Entity 指定 Schema
@Entity
@Table(name = "payments", schema = "payment")
public class PaymentJpaEntity { ... }

@Entity
@Table(name = "stocks", schema = "inventory")
public class StockJpaEntity { ... }
```

---

## 八、Port / Adapter 快速切換一覽表

| Outbound Port | Real Adapter (Testcontainers) | Fake Adapter (Unit Test) | 替代方案 Adapter |
|----------------|-------------------------------|--------------------------|-------------------|
| `OrderWriteRepository` | `JpaOrderRepository` + PostgreSQL | `InMemoryOrderRepository` | — |
| `OrderReadRepository` | `JpaOrderReadRepository` + PG | `InMemoryOrderReadRepository` | — |
| `SearchPort` | `ElasticsearchSearchAdapter` + ES | `InMemorySearchAdapter` | `JdbcSearchAdapter` (降級) |
| `CachePort` | `RedisCacheAdapter` + Redis | `InMemoryCacheAdapter` | `CaffeineLocalCacheAdapter` |
| `ObjectStoragePort` | `MinioObjectStorageAdapter` + MinIO | `InMemoryObjectStorageAdapter` | `S3ObjectStorageAdapter` (AWS) |
| `SecretProvider` | `VaultSecretProvider` + Vault | `PropertyFileSecretProvider` | `AwsSmSecretProvider` |
| `DistributedLockPort` | `RedisDistributedLockAdapter` + Redis | `ReentrantLockAdapter` (本地鎖) | `JdbcPessimisticLockAdapter` |
| `AuthPort` | `KeycloakAuthAdapter` + Keycloak | `StubAuthAdapter` (全放行) | `Auth0AuthAdapter` |

切換機制：`@Profile` + `@ConditionalOnProperty`

---

## 九、專案結構

```
ecommerce-monolith-java/
├── build.gradle.kts                        # BOM + Spring Modulith
│
├── shared-kernel/                          # 跨模組共用
│   ├── domain/
│   │   ├── Money.java                      # record
│   │   ├── Quantity.java                   # record
│   │   └── DomainEvent.java                # marker interface
│   ├── event/                              # Integration Event 定義
│   │   ├── OrderCreatedEvent.java          # record
│   │   ├── PaymentCompletedEvent.java
│   │   ├── PaymentFailedEvent.java
│   │   └── InventoryDeductedEvent.java
│   └── port/                               # 共用 Outbound Port
│       ├── ObjectStoragePort.java
│       └── SecretProvider.java
│
├── product/                                # 商品模組
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Order.java                  # Aggregate Root
│   │   │   ├── Product.java                # Entity
│   │   │   ├── OrderStatus.java            # sealed interface
│   │   │   └── vo/                         # OrderId, OrderLine, Price
│   │   ├── event/                          # 模組內部 Domain Event
│   │   ├── port/
│   │   │   ├── inbound/                    # PlaceOrderUseCase, QueryProductUseCase
│   │   │   └── outbound/                   # OrderWriteRepository, SearchPort, CachePort
│   │   └── service/                        # PricingService
│   ├── application/
│   │   ├── command/                        # PlaceOrderCommandHandler
│   │   ├── query/                          # SearchProductQueryHandler
│   │   └── eventlistener/                  # InventoryDeductedEventListener
│   ├── adapter/
│   │   ├── inbound/
│   │   │   └── rest/                       # ProductController, OrderController
│   │   └── outbound/
│   │       ├── persistence/                # JpaOrderRepository + JPA Entity 映射
│   │       ├── search/                     # ElasticsearchSearchAdapter
│   │       ├── cache/                      # RedisCacheAdapter
│   │       └── storage/                    # MinioObjectStorageAdapter
│   └── config/                             # ProductModuleConfig
│
├── payment/                                # 支付模組
│   ├── domain/
│   │   ├── model/                          # Payment, IdempotencyKey
│   │   ├── port/
│   │   │   ├── inbound/                    # ProcessPaymentUseCase
│   │   │   └── outbound/                   # PaymentWriteRepository, SecretProvider
│   │   └── service/                        # PaymentValidationService
│   ├── application/
│   │   ├── command/                        # ProcessPaymentCommandHandler
│   │   ├── query/                          # GetPaymentStatusQueryHandler
│   │   └── eventlistener/                  # OrderCreatedEventListener
│   ├── adapter/
│   │   └── outbound/
│   │       ├── persistence/                # JpaPaymentRepository
│   │       ├── storage/                    # MinioReceiptAdapter
│   │       └── secret/                     # VaultSecretProvider
│   └── config/
│
├── inventory/                              # 庫存模組
│   ├── domain/
│   │   ├── model/                          # Stock, StockLevel, Reservation
│   │   ├── port/
│   │   │   ├── inbound/                    # DeductInventoryUseCase
│   │   │   └── outbound/                   # StockWriteRepository, DistributedLockPort
│   │   └── service/                        # StockAllocationService
│   ├── application/
│   │   ├── command/                        # DeductInventoryCommandHandler
│   │   ├── query/                          # GetStockLevelQueryHandler
│   │   └── eventlistener/                  # PaymentCompletedEventListener
│   ├── adapter/
│   │   └── outbound/
│   │       ├── persistence/                # JpaStockRepository
│   │       ├── lock/                       # RedisDistributedLockAdapter
│   │       └── secret/                     # VaultSecretProvider
│   └── config/
│
├── infrastructure/                         # 全域基礎設施
│   ├── auth/                               # Keycloak Security Config
│   ├── persistence/                        # 共用 DataSource + Flyway 多 Schema
│   └── observability/                      # Micrometer, Logging
│
├── app/                                    # 啟動模組
│   ├── ECommerceApplication.java           # @SpringBootApplication
│   ├── application.yml
│   └── application-{profile}.yml
│
└── tests/
    ├── product/
    │   ├── domain/                         # 純 Unit Test，無容器
    │   ├── application/                    # InMemory Adapter
    │   └── adapter/                        # Testcontainers (PG, Redis, ES, MinIO)
    ├── payment/
    │   ├── domain/
    │   ├── application/
    │   └── adapter/                        # Testcontainers (PG, Vault, MinIO)
    ├── inventory/
    │   ├── domain/
    │   ├── application/
    │   └── adapter/                        # Testcontainers (PG, Redis, Vault)
    ├── integration/                        # 全鏈路 E2E (6 容器)
    ├── modulith/                           # Spring Modulith 驗證
    │   └── ModulithStructureTests.java
    └── architecture/                       # ArchUnit 分層規則
```

---

## 十、測試分層策略

### 測試金字塔

```
                        ╱╲
                       ╱  ╲          E2E Test
                      ╱ 少 ╲         全容器 + 全模組事件流
                     ╱──────╲        Testcontainers × 6
                    ╱        ╲
                   ╱   中等    ╲      Integration Test (per Adapter)
                  ╱            ╲     每個 Adapter 配對一個容器
                 ╱──────────────╲    Testcontainers × 1~2
                ╱                ╲
               ╱       大量       ╲   Unit Test (Domain + Application)
              ╱                    ╲  InMemory Adapter / 無容器
             ╱──────────────────────╲ 純 JUnit 5
```

### 各層測試對照

| 測試層級 | 測試對象 | 使用容器 | 速度 |
|----------|----------|----------|------|
| Unit — Domain | Aggregate 業務規則、Domain Service | 無 | <1ms/test |
| Unit — Application | CommandHandler + InMemory Adapter | 無 | <5ms/test |
| Integration — Adapter | 單一 Adapter vs 真實基礎設施 | 1~2 個 | ~1s/test |
| Integration — Module | 單模組完整流程 | 2~3 個 | ~3s/test |
| E2E — 全鏈路 | 下單 → 支付 → 扣庫存（事件串接） | 6 個 | ~10s/test |
| Modulith — 結構 | 模組邊界依賴方向 | 無 | <1s/test |

---

## 十一、章節規劃

### 第一章：專案骨架 (Day 1)

**1.1 Gradle Multi-Module 搭建**
- Java 21 toolchain 設定
- Spring Boot 4 BOM + Testcontainers BOM + Spring Modulith BOM
- Module 間依賴方向（domain → 零依賴、adapter → domain）

**1.2 Shared Kernel**
- 共用 Value Object：`Money`、`Quantity`（Java record）
- Integration Event record 定義（跨模組通訊契約）
- 共用 Outbound Port：`ObjectStoragePort`、`SecretProvider`

**1.3 Spring Modulith 設定**
- `@ApplicationModule` 標記各模組
- `ApplicationModuleTest` 驗證模組邊界
- Event Publication Log 設定

**1.4 ArchUnit 守門員**
- Domain 層不得引用 Spring / JPA annotation
- 模組 A 不得 import 模組 B 的 domain.model
- CI 中自動執行

---

### 第二章：商品模組 — Domain & CQRS (Day 2-3)

**2.1 Domain Model 建構**
- `Order` Aggregate Root、`Product` Entity
- Sealed Interface：`OrderStatus`、`OrderEvent`
- Domain Service：`PricingService`
- 純 Unit Test

**2.2 CQRS — Command Side**
- `PlaceOrderUseCase` → `PlaceOrderCommandHandler`
- `@Transactional` + `@DomainEvents` 自動發送
- Unit Test with `InMemoryOrderRepository`

**2.3 CQRS — Query Side**
- `QueryProductUseCase` → `SearchProductQueryHandler`
- `SearchPort` + `CachePort`
- Unit Test with InMemory Adapter

**2.4 Adapter — PostgreSQL**
- `JpaOrderRepository` + JPA Entity ↔ Domain 映射
- Flyway migration (product schema)
- Testcontainers：`PostgreSQLContainer`
- Contract Test：同一份測試跑 InMemory + JPA

**2.5 Adapter — Elasticsearch**
- `ElasticsearchSearchAdapter` : `SearchPort`
- Testcontainers：`ElasticsearchContainer`
- 測試：索引建立、全文檢索、facet

**2.6 Adapter — Redis**
- `RedisCacheAdapter` : `CachePort`
- Testcontainers：`GenericContainer("redis:7")`
- 測試：Cache Aside Pattern、TTL、快取失效

**2.7 Adapter — MinIO**
- `MinioObjectStorageAdapter` : `ObjectStoragePort`
- Testcontainers：`GenericContainer("minio/minio")`
- 測試：商品圖片上傳、Presigned URL

**2.8 Adapter — Keycloak**
- Spring Security OAuth2 Resource Server + JWT
- Testcontainers：`KeycloakContainer` + realm JSON import
- 測試：無 Token → 401、錯誤角色 → 403、正確 JWT → 200

---

### 第三章：支付模組 — Event Listener & Secrets (Day 4-5)

**3.1 Domain Model**
- `Payment` Aggregate Root、`IdempotencyKey` Value Object
- Sealed Interface：`PaymentStatus`、`PaymentEvent`
- Unit Test：冪等性、金額驗證

**3.2 事件接收 — @TransactionalEventListener**
- `OrderCreatedEventListener` 接收 `OrderCreatedEvent`
- `AFTER_COMMIT` 確保商品模組交易已提交
- 觸發 `ProcessPaymentCommandHandler`

**3.3 Adapter — Vault**
- `VaultSecretProvider` : `SecretProvider`
- Testcontainers：`GenericContainer("hashicorp/vault")` dev mode
- 測試：啟動時注入第三方支付 API Key
- 切換展示：`PropertyFileSecretProvider` 替代

**3.4 Adapter — MinIO (收據歸檔)**
- 複用 `ObjectStoragePort`，展示同一 Port 跨模組複用
- 支付完成 → 上傳收據 PDF → 回寫 receipt URL

**3.5 事件發送**
- 支付成功 → `PaymentCompletedEvent`
- 支付失敗 → `PaymentFailedEvent`（觸發補償）

---

### 第四章：庫存模組 — 併發控制 (Day 5-6)

**4.1 Domain Model**
- `Stock` Aggregate Root（含 version 樂觀鎖）
- `StockLevel`、`Reservation` Value Object
- Domain Service：`StockAllocationService`
- Unit Test：庫存不足拋 `DomainException`

**4.2 事件接收**
- `PaymentCompletedEventListener` 接收 `PaymentCompletedEvent`
- 觸發 `DeductInventoryCommandHandler`

**4.3 Adapter — Redis 分散式鎖**
- `RedisDistributedLockAdapter` : `DistributedLockPort`
- Testcontainers：Redis Container
- 測試：併發扣庫存 → 鎖保護 → 最終庫存正確
- 切換展示：`JdbcPessimisticLockAdapter`（SELECT FOR UPDATE）

**4.4 Adapter — Vault 動態密鑰**
- Vault Database Secret Engine → 臨時 DB 帳密
- 測試：lease renewal、credential rotation

**4.5 事件發送**
- 扣庫存成功 → `InventoryDeductedEvent`
- 庫存不足 → `InventoryDeductionFailedEvent`（觸發退款補償）

---

### 第五章：全鏈路整合 (Day 7)

**5.1 Singleton Container 基礎設施**
- `SharedContainers` 類別統一管理 6 個容器
- 容器啟動順序與健康檢查
- `@SpringBootTest` + `@DynamicPropertySource` 注入連線

**5.2 Happy Path E2E**
- Keycloak 取得 JWT → POST /api/orders
  → ApplicationEventPublisher: OrderCreatedEvent
  → 支付模組處理 (Vault + MinIO)
  → ApplicationEventPublisher: PaymentCompletedEvent
  → 庫存模組扣庫存 (Redis Lock)
  → ApplicationEventPublisher: InventoryDeductedEvent
  → 商品模組更新訂單 + ES + Redis
- `Awaitility.await()` 等待非同步事件收斂

**5.3 Saga 補償測試**
- 支付成功但庫存不足 → InventoryDeductionFailedEvent → 退款
- Spring Modulith Event Publication 重發測試
- 最終一致性驗證

**5.4 Profile 切換展示**
- `test-real`：6 個容器全啟動
- `test-lite`：移除 ES + Redis，使用 InMemory Adapter
- 展示 Port/Adapter 切換彈性

---

### 第六章：模組邊界驗證 & 進階 (Day 8)

**6.1 Spring Modulith 結構驗證**
- `ApplicationModuleTest` 驗證模組間依賴方向
- 事件合規性：模組只能監聽已宣告的事件
- 文件自動產生：模組關係圖、事件流程圖

**6.2 ArchUnit 補充規則**
- Domain 不依賴 Spring Framework
- Adapter 不得互相依賴
- 所有 Outbound Port 必須有至少一個實作

**6.3 Contract Test — Port 行為契約**
- 每個 Outbound Port 一套 Contract Test
- InMemory Adapter 和 Real Adapter 各跑一次
- 確保 Fake 與 Real 行為一致

**6.4 BDD Cucumber 整合**
```gherkin
Feature: 商品訂購流程

  Scenario: 成功訂購並扣庫存
    Given 商品 "iPhone 16" 庫存為 100
    And 使用者 "buyer01" 已通過 Keycloak 認證
    When 使用者下單購買 1 件 "iPhone 16"
    And 支付成功
    Then 庫存應減少為 99
    And 訂單狀態應為 "COMPLETED"
    And 支付憑證應存在於 MinIO

  Scenario: 庫存不足觸發退款
    Given 商品 "iPhone 16" 庫存為 0
    When 使用者下單購買 1 件 "iPhone 16"
    And 支付成功
    Then 應觸發退款流程
    And 訂單狀態應為 "REFUNDED"
```

**6.5 未來拆分微服務路徑**
```
Step 1: Schema 隔離 → 獨立 PostgreSQL 實例（已無跨 Schema 查詢）
Step 2: ApplicationEventPublisher → KafkaEventPublisher（換 Adapter）
Step 3: 各模組獨立打包為容器 + API Gateway
```

---

## 十二、可行性與風險評估

| 面向         | 評估 |
|--------------|------|
| 技術可行性   | ✅ Spring Modulith + ApplicationEventPublisher 是官方推薦方案 |
| 容器精簡     | ✅ 6 個容器，比微服務版少 2 個 |
| 事件可靠性   | ✅ Spring Modulith Event Publication Log 保障 at-least-once |
| 模組邊界     | ✅ Spring Modulith 自動驗證 + ArchUnit 雙重保障 |
| 未來可拆分   | ✅ 交易獨立 + 事件驅動 + Port/Adapter，拆分只換接線 |
| 學習曲線     | ⚠️ Spring Modulith 概念需額外學習，但官方文件完整 |
| 硬體需求     | ⚠️ 6 容器 + IDE → 建議 12GB+ RAM |
| 開發時間     | 完整教程含程式碼約 8 個工作天 |
