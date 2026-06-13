# 電商模組化單體 — Spring Boot 4 + Spring Modulith + Testcontainers 教程

一份從零開始的教學專案,示範如何用 **Java 21** + **Spring Boot 4.1** + **Spring Modulith** + **Testcontainers**,打造一個「**長得像微服務、但只用單一進程部署**」的模組化單體電商系統。

> 對應的設計文件在 `../testcontainers-monolith-java.md`,本 README 是給「想實際跑起來看看」的學習者用的入門指南。

---

## 目錄

1. [這個專案在做什麼](#1-這個專案在做什麼)
2. [學會這個專案,你會帶走什麼](#2-學會這個專案你會帶走什麼)
3. [環境準備](#3-環境準備)
4. [五分鐘上手](#4-五分鐘上手)
5. [核心架構概念](#5-核心架構概念)
6. [專案結構導覽](#6-專案結構導覽)
7. [測試策略](#7-測試策略)
8. [六大章節對應的程式碼位置](#8-六大章節對應的程式碼位置)
9. [常見問題與排錯](#9-常見問題與排錯)
10. [學完之後可以做什麼](#10-學完之後可以做什麼)

---

## 1. 這個專案在做什麼

想像一個小型電商系統,使用者下單後要經過三個步驟:

```
   下單 ──→ 扣款 ──→ 扣庫存 ──→ 訂單完成
```

**傳統做法**:把這三件事拆成三個微服務,各自有 DB、各自部署、用 Kafka 串接事件。
**問題**:對小團隊來說,管 3 個服務 + Kafka 太貴,維運成本遠大於業務複雜度。

**這個專案的做法 — 模組化單體 (Modular Monolith)**

把三個業務切成「邏輯模組」放進同一個 Spring Boot 應用裡,但保持以下紀律:

```
✅ 模組之間只透過 Spring 的 ApplicationEventPublisher 發事件溝通
✅ 每個模組擁有自己的 DB Schema (product / payment / inventory)
✅ 模組 A 禁止 import 模組 B 的 Domain Model
✅ 每模組獨立交易 (絕不跨模組 @Transactional)
```

這樣的設計讓你 **今天用單體享受開發效率,明天需要時可以幾乎無痛拆成微服務**(只要把 ApplicationEventPublisher 換成 KafkaTemplate,模組打包成獨立 jar 就行)。

---

## 2. 學會這個專案,你會帶走什麼

| 帶得走的概念 | 為什麼重要 |
|---|---|
| **Hexagonal Architecture(六角形/Port-Adapter)** | 業務邏輯與框架解耦,換 Redis 改 Caffeine 不必動 Domain |
| **DDD 戰術模式** (Aggregate、Value Object、Domain Event) | 用領域語言寫程式,而不是被 ORM 牽著走 |
| **CQRS** (Command/Query 分離) | 寫入 (Order) 與查詢 (Search) 用不同模型,各自最佳化 |
| **Spring Modulith** | Spring 官方的模組邊界驗證工具,自動產出模組依賴圖 |
| **Testcontainers** | 用真實的 PostgreSQL/Redis/ES 跑整合測試,不靠 mock |
| **Saga 補償** | 庫存不足時自動退款,事件驅動的最終一致性 |
| **ArchUnit** | 用程式碼描述「禁止 Domain 依賴 Spring」這類規則,CI 自動把關 |
| **BDD with Cucumber** | 用業務語言寫測試 (繁體中文 Gherkin) |

---

## 3. 環境準備

### 必裝

| 工具 | 版本 | 用途 |
|---|---|---|
| **JDK 21** | 21+ | 編譯與執行 (用了 record / sealed interface) |
| **Gradle** | 8.14+ (或用內附 wrapper) | 建置工具 |

### 跑容器整合測試 / 本機完整啟動才需要

| 工具 | 版本 | 用途 |
|---|---|---|
| **Docker** | 20+ | Testcontainers / docker-compose 都要 |
| **Docker Compose** | v2+ | 本機起 6 個服務 |
| **curl** | 任意 | 跑 vault-bootstrap 腳本用 |

### 確認環境就緒

```bash
java -version    # 應該看到 21
docker info      # 確認 Docker daemon 在跑
```

---

## 4. 五分鐘上手

### Step 1 — 只跑單元測試 (不用 Docker)

```bash
./gradlew test
```

會跑完所有 Domain + Application + ArchUnit + Modulith 邊界驗證,大約 30 秒,**全綠**就代表骨架沒問題。

### Step 2 — 跑 Testcontainers 整合測試 (需要 Docker)

```bash
./gradlew integrationTest
```

這會啟動 PostgreSQL / Redis / Elasticsearch / MinIO / Vault / Keycloak 共 6 個容器,跑各 Adapter 的整合測試和全鏈路 E2E。第一次拉 image 會比較久。

### Step 3 — 本機跑起來打 API

```bash
# 1. 起所有基礎設施
docker compose up -d

# 2. 把支付 API key 寫入 Vault
./scripts/vault-bootstrap.sh

# 3. 等 30 秒讓 Keycloak 完全啟動,然後跑 app
./gradlew :app:bootRun
```

打 API:

```bash
# 取得 access token
TOKEN=$(curl -sS -X POST \
  http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'grant_type=password&client_id=ecommerce-web&username=buyer01&password=secret' \
  | jq -r .access_token)

# 查商品
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/products?q=iPhone

# 下單 (需要先用 stockAdminUseCase 種商品 + 庫存,
# 教程裡通常在測試的 @BeforeEach 做,真實要寫個 admin endpoint)
```

### Step 4 — 產出 Modulith 文件

```bash
./gradlew :app:syncModulithDocs
```

`docs/spring-modulith/components.puml` 是整體 C4 圖,可貼進 [PlantUML Server](https://www.plantuml.com/plantuml) 視覺化。

---

## 5. 核心架構概念

### 5.1 Hexagonal Architecture (六角形/Port-Adapter)

每個業務模組 (product/payment/inventory) 都是這樣的同心圓結構:

```
   ┌─────────────────────────────────────────────┐
   │              Adapter (外圍)                  │ 跟具體技術綁:JPA、Redis、REST
   │  ┌───────────────────────────────────────┐  │
   │  │         Application (中圈)              │  │ 串接、交易邊界、CQRS Handler
   │  │  ┌─────────────────────────────────┐  │  │
   │  │  │        Domain (核心)             │  │  │ 純 Java、業務規則、Port 介面
   │  │  └─────────────────────────────────┘  │  │
   │  └───────────────────────────────────────┘  │
   └─────────────────────────────────────────────┘

   依賴方向:外圍 → 中圈 → 核心 (反方向禁止)
```

具體到目錄:

```
product/
└─ src/main/java/com/tutorial/ecommerce/product/
   ├─ domain/         ← 純 Java,禁止 import Spring
   │  ├─ model/       ← Order, Product, OrderStatus
   │  ├─ port/
   │  │  ├─ inbound/  ← PlaceOrderUseCase (應用要做的事)
   │  │  └─ outbound/ ← OrderWriteRepository, SearchPort (要從外面拿的能力)
   │  ├─ event/       ← 模組內部 Domain Event
   │  └─ service/     ← PricingService (跨 Aggregate 的純函式)
   ├─ application/    ← 可以 import Spring,但只能用 @Service / @Transactional
   │  ├─ command/     ← PlaceOrderCommandHandler
   │  ├─ query/       ← SearchProductQueryHandler
   │  └─ eventlistener/ ← 監聽其他模組的事件
   └─ adapter/        ← 跟外部技術掛勾
      ├─ inbound/
      │  └─ rest/     ← OrderController, ProductController
      └─ outbound/
         ├─ persistence/ ← JpaOrderRepository (實作 OrderWriteRepository)
         ├─ cache/       ← RedisCacheAdapter
         ├─ search/      ← ElasticsearchSearchAdapter
         └─ storage/     ← MinioObjectStorageAdapter
```

**規則由 ArchUnit 自動把關**,寫錯方向 CI 就紅。看 `app/src/test/java/com/tutorial/ecommerce/architecture/LayerArchitectureTest.java`。

### 5.2 DDD 戰術模式

```java
// Value Object:不可變、由值判等
public record Money(BigDecimal amount, Currency currency) { ... }

// Aggregate Root:有 ID、保護業務不變性、累積 Domain Event
public class Order {
    public void markPaid(PaymentId paymentId) {
        if (!(status instanceof OrderStatus.Created)) {
            throw new DomainException("only CREATED orders can be marked paid");
        }
        status = new OrderStatus.Paid(Instant.now(), paymentId);
        domainEvents.add(new OrderDomainEvent.OrderPaid(id, paymentId, now));
    }
}

// Sealed Interface 表達狀態機:編譯器幫你檢查 switch 是否窮盡
public sealed interface OrderStatus
    permits Created, Paid, Completed, Cancelled, Refunded { ... }
```

看 `product/src/main/java/com/tutorial/ecommerce/product/domain/model/Order.java`。

### 5.3 CQRS — 讀寫分離

```
寫入路徑 (Command)               讀取路徑 (Query)
─────────────────                ─────────────────
PlaceOrderUseCase                QueryProductUseCase
        │                                │
PlaceOrderCommandHandler         SearchProductQueryHandler
        │                                │
   Order Aggregate (DDD)         ProductView (DTO)
        │                                │
   JPA Repository                   Elasticsearch + Redis Cache
```

寫入用 Domain Model 強型別、嚴格驗證;查詢用 DTO + 快取,直接服務前端。兩條路徑互不干擾。

### 5.4 模組之間的通訊 — 事件驅動

跨模組 **嚴禁** 直接呼叫對方 Service。所有跨模組溝通走 `shared-kernel` 裡定義的 Integration Event:

```
[商品模組] 訂單成立
    │ applicationEventPublisher.publishEvent(new OrderCreatedEvent(...))
    ▼
[支付模組] @ApplicationModuleListener void handle(OrderCreatedEvent e)
    │ 扣款 → publishEvent(new PaymentCompletedEvent(...))
    ▼
[庫存模組] @ApplicationModuleListener void handle(PaymentCompletedEvent e)
    │ 扣庫存 → publishEvent(new InventoryDeductedEvent(...))
    ▼
[商品模組] 收到 InventoryDeductedEvent → 訂單轉 COMPLETED
```

`@ApplicationModuleListener` 等同於:
- `@TransactionalEventListener(phase = AFTER_COMMIT)` — 等前一筆交易 commit 才觸發
- `@Async` — 非同步執行,不阻塞發送端
- `@Transactional` — 自己開新交易
- **Spring Modulith Event Publication Log** — 自動持久化事件,失敗會重發 (at-least-once)

看支付模組的 listener:`payment/src/main/java/com/tutorial/ecommerce/payment/application/eventlistener/OrderCreatedEventListener.java`。

### 5.5 Spring Modulith 是什麼

它是 Spring 官方的「模組化單體」支援框架,做三件事:

1. **`@ApplicationModule`** — 把 package 標為模組。每個業務 package 的 `package-info.java` 都有這個註解。
2. **`ApplicationModules.verify()`** — 啟動時自動驗證模組依賴方向,違規測試紅。看 `app/src/test/java/com/tutorial/ecommerce/modulith/ModulithStructureTest.java`。
3. **`Documenter`** — 從程式碼分析模組關係,自動產 PlantUML / AsciiDoc 文件。

對應的 OPEN 標記:`shared-kernel` 標 `type = OPEN`,代表它是「所有人都可以引用的共用核」(VO + Event),不受嚴格的 NamedInterface 限制。

### 5.6 Testcontainers 為什麼重要

過去整合測試的兩種爛選擇:

```
A. 用 H2 / mock 跑測試 → 快,但跟 prod 行為不一致 → 上線出包
B. 開一台共用 dev DB → 慢、會互相干擾、CI 排隊
```

Testcontainers 給你第三條路:**測試一啟動就拉真的 PostgreSQL Docker container,結束就清掉**。看 `product/src/test/java/com/tutorial/ecommerce/product/adapter/persistence/JpaOrderRepositoryIT.java`:

```java
@Container
static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

@DynamicPropertySource
static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    ...
}
```

整個 E2E 測試起 6 個容器 (Postgres / Redis / ES / MinIO / Vault / Keycloak),用 `SharedContainers` 做 singleton 共用,看 `app/src/test/java/com/tutorial/ecommerce/e2e/SharedContainers.java`。

---

## 6. 專案結構導覽

```
ecommerce-monolith-java/
├─ build.gradle.kts                根 build,所有子模組共用設定
├─ settings.gradle.kts             宣告 6 個子模組
├─ gradle/libs.versions.toml       版本目錄,集中管理所有依賴版本
├─ gradle.properties
├─ docker-compose.yml              本機開發起 6 個基礎服務
├─ scripts/vault-bootstrap.sh      docker-compose 起來後,塞 vault secret
│
├─ shared-kernel/                  跨模組共用 (VO + Integration Event + 通用 Port)
│  └─ src/main/java/com/tutorial/ecommerce/sharedkernel/
│     ├─ domain/                   Money, Quantity, OrderId, PaymentId, ProductId, UserId
│     ├─ event/                    OrderCreatedEvent, PaymentCompletedEvent, ...
│     └─ port/                     ObjectStoragePort, SecretProvider
│
├─ product/                        商品/訂單模組
│  └─ src/main/.../product/
│     ├─ domain/                   Order Aggregate, Product Entity, PricingService
│     ├─ application/              PlaceOrderCommandHandler, SearchProductQueryHandler
│     ├─ adapter/
│     │  ├─ inbound/rest/          OrderController, ProductController
│     │  └─ outbound/
│     │     ├─ persistence/        JpaOrderRepository (+ JPA Entity 映射)
│     │     ├─ search/             ElasticsearchSearchAdapter
│     │     ├─ cache/              RedisCacheAdapter
│     │     └─ storage/            MinioObjectStorageAdapter
│     └─ src/main/resources/db/migration/product/  Flyway migration
│
├─ payment/                        支付模組
│  └─ ... 同樣結構,Adapter: JPA, Vault, MinIO 收據, PaymentGateway
│
├─ inventory/                      庫存模組
│  └─ ... 同樣結構,Adapter: JPA, Redis 分散式鎖
│
├─ infrastructure/                 全域基礎設施 (不是業務模組)
│  └─ auth/SecurityConfig.java     OAuth2 Resource Server
│  └─ persistence/JpaSchemaIsolationConfig.java  共用 DataSource、隔離 Schema
│
├─ app/                            啟動模組
│  └─ src/main/java/com/tutorial/ecommerce/ECommerceApplication.java   @Modulithic
│  └─ src/main/resources/
│     ├─ application.yml                    預設設定
│     ├─ application-test-real.yml          6 容器全開
│     └─ application-test-lite.yml          InMemory adapter,只開 PG
│  └─ src/test/java/com/tutorial/ecommerce/
│     ├─ architecture/              ArchUnit 規則 (16 條)
│     ├─ modulith/                  Spring Modulith 邊界驗證 + Documenter
│     ├─ e2e/                       SharedContainers, PlaceOrderE2EIT, InventoryShortageSagaIT
│     └─ bdd/                       Cucumber + StepDef (中文 Gherkin)
│
└─ docs/spring-modulith/           ./gradlew :app:syncModulithDocs 產出的模組關係文件
```

---

## 7. 測試策略

```
                        ╱╲
                       ╱  ╲          E2E IT (需 Docker, 跑 1-2 個)
                      ╱ 慢 ╲          6 容器 + 全鏈路事件流
                     ╱──────╲        Awaitility 等最終一致性
                    ╱        ╲
                   ╱ Adapter ╲       Per-Adapter IT (需 Docker, 跑很多)
                  ╱   IT      ╲      JPA / Redis / ES / MinIO / Vault 各自一支
                 ╱──────────────╲
                ╱                ╲
               ╱  Application     ╲  CommandHandler + InMemory Adapter (秒級)
              ╱  Unit Test         ╲
             ╱──────────────────────╲
            ╱   Domain Unit Test     ╲ 純 Java, <1ms/test, 沒有任何框架
           ╱──────────────────────────╲
          ╱   ArchUnit + Modulith      ╲ 守護規則, 沒有業務行為
         ╱──────────────────────────────╲
```

| 跑法 | 範圍 | 需要 Docker? |
|---|---|---|
| `./gradlew test` | Domain + Application + 結構 (預設不含 `*IT`、不含 `bdd/`) | ❌ 不用 |
| `./gradlew integrationTest` | 含 Docker 的 `*IT` | ✅ 要 |
| `./gradlew check` | `test` + `integrationTest` | ✅ 要 |
| `./gradlew :app:bootJar` | 打包成可執行 jar | ❌ |
| `./gradlew :app:bootRun` | 啟動 app (需先 `docker compose up`) | ✅ |

**寫測試的習慣建議**:
- Domain 規則 → 純 JUnit + AssertJ
- Application 流程 → InMemory Adapter (`*/test/java/.../application/fake/`)
- Adapter 行為 → 配對一個 Testcontainer (`*IT.java`)
- 跨模組事件鏈 → `@SpringBootTest` + `SharedContainersInitializer`
- 業務規則 → Cucumber feature file (`app/src/test/resources/features/`)

---

## 8. 六大章節對應的程式碼位置

對應 `../testcontainers-monolith-java.md` 的章節編號:

### 第一章 — 專案骨架

| 主題 | 檔案 |
|---|---|
| Gradle 多模組設定 | `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` |
| Spring Modulith 啟動標記 | `app/src/main/java/com/tutorial/ecommerce/ECommerceApplication.java` |
| `@ApplicationModule` 標記 | 各模組的 `package-info.java` |
| ArchUnit 守門員 | `app/src/test/java/com/tutorial/ecommerce/architecture/LayerArchitectureTest.java` |
| Modulith 結構驗證 | `app/src/test/java/com/tutorial/ecommerce/modulith/ModulithStructureTest.java` |

### 第二章 — 商品模組

| 主題 | 檔案 |
|---|---|
| Order Aggregate + sealed OrderStatus | `product/src/main/java/.../domain/model/Order.java` |
| Domain unit test | `product/src/test/java/.../domain/OrderTest.java` |
| CQRS Command Handler | `product/src/main/java/.../application/command/PlaceOrderCommandHandler.java` |
| InMemory Adapter (測試用) | `product/src/test/java/.../application/fake/InMemoryOrderRepository.java` |
| JPA Adapter + Testcontainer | `product/src/main/java/.../adapter/outbound/persistence/JpaOrderRepository.java`, `product/src/test/java/.../adapter/persistence/JpaOrderRepositoryIT.java` |
| Elasticsearch Adapter | `product/src/main/java/.../adapter/outbound/search/ElasticsearchSearchAdapter.java` |
| Redis Cache Adapter | `product/src/main/java/.../adapter/outbound/cache/RedisCacheAdapter.java` |
| MinIO Adapter | `product/src/main/java/.../adapter/outbound/storage/MinioObjectStorageAdapter.java` |
| REST Controller + JWT | `product/src/main/java/.../adapter/inbound/rest/OrderController.java` |

### 第三章 — 支付模組

| 主題 | 檔案 |
|---|---|
| Payment + IdempotencyKey + sealed PaymentStatus | `payment/src/main/java/.../domain/model/` |
| 監聽 OrderCreatedEvent | `payment/src/main/java/.../application/eventlistener/OrderCreatedEventListener.java` |
| 處理付款 + 寫收據到 MinIO | `payment/src/main/java/.../application/command/ProcessPaymentCommandHandler.java` |
| Vault Secret Provider | `payment/src/main/java/.../adapter/outbound/secret/VaultSecretProvider.java` |
| Property File 替代 Adapter | `payment/src/main/java/.../adapter/outbound/secret/PropertyFileSecretProvider.java` |
| 補償退款 listener | `payment/src/main/java/.../application/eventlistener/InventoryDeductionFailedEventListener.java` |

### 第四章 — 庫存模組

| 主題 | 檔案 |
|---|---|
| Stock Aggregate (樂觀鎖 + reserve/release/confirm) | `inventory/src/main/java/.../domain/model/Stock.java` |
| 監聽 PaymentCompletedEvent | `inventory/src/main/java/.../application/eventlistener/PaymentCompletedEventListener.java` |
| Redis 分散式鎖 | `inventory/src/main/java/.../adapter/outbound/lock/RedisDistributedLockAdapter.java` |
| 本地鎖替代 (`@Profile` 切換) | `inventory/src/main/java/.../adapter/outbound/lock/ReentrantLockAdapter.java` |
| 跨事件取訂單 lines (Snapshot Listener) | `inventory/src/main/java/.../application/eventlistener/OrderCreatedSnapshotListener.java` |

### 第五章 — 全鏈路 E2E

| 主題 | 檔案 |
|---|---|
| 6 容器 singleton | `app/src/test/java/com/tutorial/ecommerce/e2e/SharedContainers.java` |
| Happy Path E2E | `app/src/test/java/com/tutorial/ecommerce/e2e/PlaceOrderE2EIT.java` |
| Saga 補償 (退款) | `app/src/test/java/com/tutorial/ecommerce/e2e/InventoryShortageSagaIT.java` |
| Keycloak realm 設定 | `app/src/test/resources/keycloak/ecommerce-realm.json` |
| test-real / test-lite profile | `app/src/main/resources/application-test-{real,lite}.yml` |

### 第六章 — 進階

| 主題 | 檔案 |
|---|---|
| Port Contract Test | `product/src/test/java/.../contract/OrderRepositoryContract.java` |
| Event 架構規則 | `app/src/test/java/.../architecture/EventArchitectureTest.java` |
| Cucumber feature (繁中) | `app/src/test/resources/features/place_order.feature` |
| Cucumber StepDef | `app/src/test/java/com/tutorial/ecommerce/bdd/PlaceOrderSteps.java` |
| Cucumber Spring context | `app/src/test/java/com/tutorial/ecommerce/bdd/CucumberSpringContext.java` |

---

## 9. 常見問題與排錯

### Q1. 跑 `./gradlew test` 失敗,說 Docker 連不上

預設 `test` task 已經排除 `*IT.class`,如果還是看到 Docker 錯誤,可能是某支 `Test.java` 不小心引入了 `@Testcontainers`。看 `build.gradle.kts:70-73` 的 exclude 設定。

### Q2. ArchUnit 規則一直紅

預設規則對「空集合」也會報錯,新增的 package 還沒類別會誤報。看 `app/src/test/resources/archunit.properties`:

```
archRule.failOnEmptyShould=false
```

Layered Architecture 另外用 `.withOptionalLayers(true)`。新增業務類別後規則會自動生效。

### Q3. Modulith 報「Module X depends on non-exposed type from Y」

要嘛 (a) 你違規跨模組存取了內部 package,(b) 你想公開的型別還沒標 NamedInterface。

`shared-kernel` 標了 `type = OPEN`,所有 sub-package 都對外公開。一般業務模組要用 `@org.springframework.modulith.NamedInterface` 標 sub-package 才會被允許跨模組引用。

### Q4. 編譯通過但 IDE 紅一片

跑一次 `./gradlew :app:bootJar` 讓 IntelliJ 重新同步索引;或者 *File → Invalidate Caches and Restart*。

Lombok 註解處理器需要在 IDE 安裝 Lombok plugin (本專案 build 用了 Lombok 但程式碼中沒實際大量用)。

### Q5. 我要怎麼新增一個業務模組?

1. 在 `settings.gradle.kts` 加 `include("notification")`
2. 建立 `notification/build.gradle.kts` (參考其他模組)
3. 建立 `notification/src/main/java/com/tutorial/ecommerce/notification/package-info.java`,標 `@ApplicationModule(displayName="Notification")`
4. 想接收事件 → 寫 `@ApplicationModuleListener` listener
5. 想發送事件 → 在 `shared-kernel/event/` 加新的 Integration Event record
6. 重跑 `./gradlew :app:test` — ModulithStructureTest 會自動把它納入驗證

### Q6. Spring Boot 4 跟 3 差很多嗎?

差不少,本專案踩過的坑:
- `@EntityScan` 移到 `org.springframework.boot.persistence.autoconfigure`
- `@DataJpaTest` 移到 `org.springframework.boot.data.jpa.test.autoconfigure`,artifact 是 `spring-boot-data-jpa-test`
- `@WebMvcTest` 在 `spring-boot-webmvc-test`
- `TestRestTemplate` 在 `org.springframework.boot.resttestclient`

`gradle/libs.versions.toml` 註解了所有新位置,可當對照表。

### Q7. 容器啟動很慢/卡住

```bash
# 看 testcontainers debug log
TESTCONTAINERS_REUSE_ENABLE=true ./gradlew integrationTest

# 看哪個容器卡住
docker ps -a
docker logs <container_id>
```

Elasticsearch 第一次啟動約 30-60 秒、Keycloak 約 20 秒,屬正常。

---

## 10. 學完之後可以做什麼

### 馬上可以練手

- **加一個 Notification 模組**:訂單完成後寄 Email,用 `@ApplicationModuleListener` 接 `OrderCompletedEvent`
- **加 Admin REST API**:`POST /api/admin/products` 建商品 + 種庫存,讓本機開發不用一直跑 SQL
- **用 OpenAPI**:加 `springdoc-openapi-starter-webmvc-ui`,自動產 Swagger UI
- **加 OpenTelemetry**:看跨模組事件的追蹤鏈,理解 AFTER_COMMIT 的時序

### 中階主題

- **跨模組 Saga 模式**:現在的補償是「庫存失敗 → 退款」,實作完整的 Order Saga 並把狀態機落地到 DB
- **Outbox Pattern**:Spring Modulith Event Publication Log 就是 Outbox 的官方實作,可以研究它怎麼用 polling 重發失敗事件
- **Read Model 投影**:用 `@EventListener` 把 Order 事件投影成獨立的查詢用 read model,徹底分離寫讀

### 進階拆分微服務

如同教程設計目標,以下三步可以「無痛拆」:

```
Step 1: 把 product schema 搬到獨立 PostgreSQL instance
       (已經 schema 隔離了,只是換連線字串)

Step 2: 把 ApplicationEventPublisher 換成 KafkaTemplate
       (寫一個 KafkaEventPublisher 實作同一個介面;
        @ApplicationModuleListener 換成 @KafkaListener)

Step 3: 把每個模組打包成獨立 jar / Docker image,前面加 API Gateway
       (Domain / Application 程式碼幾乎不動)
```

教程把這條路的所有預先工作都做了,需要時把 [[step-by-step migration guide]] 補上即可。

---

## 附錄 A — 常用指令速查

```bash
# 測試
./gradlew test                            # 全部單元測試 + ArchUnit (~30秒)
./gradlew :product:test                   # 只跑 product 模組
./gradlew :product:test --tests "OrderTest"  # 只跑特定測試類
./gradlew integrationTest                 # 含 Docker 的 IT
./gradlew check                           # test + integrationTest

# 建置
./gradlew build                           # 編譯 + 全測試
./gradlew :app:bootJar                    # 打包成可執行 jar (app/build/libs/)
./gradlew :app:bootRun                    # 直接跑 (需 docker compose 起來)

# 文件
./gradlew :app:syncModulithDocs           # 產出模組關係 PlantUML 到 docs/

# 維護
./gradlew dependencies                    # 看依賴樹
./gradlew :product:dependencies           # 看單一模組
./gradlew --refresh-dependencies          # 強制重抓依賴
./gradlew clean                           # 清掉 build/
```

## 附錄 B — Profile 對照表

| Profile | DB | Cache | Search | Storage | Secret | Lock |
|---|---|---|---|---|---|---|
| (預設) | PostgreSQL | Redis | Elasticsearch | MinIO | Vault | Redis |
| `test-real` | PG container | Redis container | ES container | MinIO container | Vault container | Redis container |
| `test-lite` | PG container | InMemory | InMemory | InMemory | PropertyFile | Reentrant |

切換方式:`./gradlew bootRun -Dspring.profiles.active=test-lite`,或在 IDE Run config 設 `SPRING_PROFILES_ACTIVE=test-lite`。

---

**Happy learning! 有任何問題歡迎開 issue 討論。**
