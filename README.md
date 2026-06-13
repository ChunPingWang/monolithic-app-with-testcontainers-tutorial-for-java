# Java/Spring Boot 4 單體應用 — Testcontainers 學習專案

這是一份**學習導向**的教程專案,目標是讓你親手寫過後,能回答四個問題:

1. **單體應用要怎麼蓋,才不會三個月後變成爛泥球?**
2. **DDD 的 Bounded Context 是什麼?在一個 Spring Boot 程式裡要怎麼劃分、怎麼讓邊界不被打破?**
3. **跨邊界要溝通,Event Bus 是怎麼運作的?事件什麼時候發、什麼時候會丟?怎麼測?**
4. **Testcontainers 怎麼用才漂亮?什麼時候該起容器、什麼時候不該?**

我們用一個小型電商(下單 → 扣款 → 扣庫存)當實際場景,把上面四件事一次練完。

> 對應的設計文件:[`docs/design/testcontainers-monolith-java.md`](docs/design/testcontainers-monolith-java.md),本 README 是給「實際 clone 下來跟著做」的學習者。

---

## 目錄

1. [這個專案要教你的事](#1-這個專案要教你的事)
2. [先想清楚:為什麼選單體?](#2-先想清楚為什麼選單體)
3. [核心概念一:DDD Bounded Context](#3-核心概念一ddd-bounded-context)
4. [核心概念二:Event Bus 跨 Context 通訊](#4-核心概念二event-bus-跨-context-通訊)
5. [核心概念三:Testcontainers 測試技巧](#5-核心概念三testcontainers-測試技巧)
6. [環境準備與五分鐘上手](#6-環境準備與五分鐘上手)
7. [專案結構導覽](#7-專案結構導覽)
8. [測試策略](#8-測試策略)
9. [常見問題與排錯](#9-常見問題與排錯)
10. [延伸學習](#10-延伸學習)

---

## 1. 這個專案要教你的事

學完之後,你會帶走這四件武器:

| # | 武器 | 解決什麼問題 |
|---|---|---|
| 1 | **用 Bounded Context 切單體** | 程式碼放對位置,新人 onboard 不用迷路;業務需求變動時,影響範圍是清楚的 |
| 2 | **Ubiquitous Language(在地語言)** | RD 跟 PM 在同一張白紙上講同一件事,不再「你說的 Order 是哪個 Order?」 |
| 3 | **Event Bus 跨 Context 溝通** | Context 之間不直接呼叫對方,用事件解耦;失敗、重發、最終一致性都有處理 |
| 4 | **Testcontainers 整合測試** | 用真實的 PostgreSQL / Redis / Elasticsearch 跑測試,不靠 mock,測試綠了就敢上 prod |

另外順便會接觸到的(不是主軸,但會出現):
- Hexagonal Architecture(Port / Adapter)— 用來實作 bounded context 內部分層
- CQRS — 讀寫分離,在這個專案是「自然出現」而不是「為了用而用」
- Spring Modulith — Spring 官方的工具,幫你**強制**遵守 Bounded Context 規則

---

## 2. 先想清楚:為什麼選單體?

很多文章把單體寫得像萬惡的怪物,實際上 **單體是新專案的最佳起點**。理由:

```
微服務的痛(從一開始就有):              單體的痛(成長後才有):
  ✗ 跨服務交易 → 永遠講不清的最終一致性     ✗ Big Ball of Mud,程式碼互相纏繞
  ✗ 分散式追蹤 / 觀測成本                  ✗ 任何小改都得跑全套測試
  ✗ 部署協調 / 版本依賴                    ✗ 換技術棧痛苦
  ✗ DB 拆 → 跨表查詢沒了
  ✗ 維運複雜度 × N 倍
```

**選單體的時機**:團隊 < 10 人、需求還在演化、產品還沒驗證 PMF。
**單體做對了什麼會死**:沒有界線、所有 Service 互相 import、一個小改動可能炸到三個業務。

**這個專案就是在教你「做對」的單體** — 邊界清楚、溝通受控、可以一個人改一個 context 不會炸到別人。

至於什麼時候要拆微服務?那是上線後業務真的長大才考慮的事,本專案會留好「將來拆得動」的接縫,但不會把「拆」當成終點。

---

## 3. 核心概念一:DDD Bounded Context

### 3.1 什麼是 Bounded Context

DDD 對於系統設計最重要的洞見是:**同一個詞,在不同業務區段意思不一樣**。

舉本專案的例子,「**訂單**」這個詞:

| 業務區段 | 「訂單」是什麼 | 關心什麼 |
|---|---|---|
| 商品 / Catalog 區段 | 一群 OrderLine(商品+數量+單價) + 買家 + 狀態機 | 商品有沒有貨、單價對不對 |
| 支付 / Payment 區段 | 一筆要扣的款項 + 冪等鍵 | 扣款成功了沒、收據在哪 |
| 庫存 / Inventory 區段 | 觸發扣庫存的指令 + 預扣記錄 | 商品庫存夠不夠、扣完寫回 DB |

如果你硬要做一個 `Order` 類別同時滿足三個區段,類別會爆炸,改一個區段會弄壞另外兩個。

**Bounded Context 的核心做法**:**每個區段擁有自己版本的「訂單」**,各自只關心自己的事,跨區段用事件 / DTO 傳訊(下一節)。

### 3.2 本專案怎麼切 Context

```
┌─────────────────┐    OrderCreatedEvent    ┌─────────────────┐
│  Product        │ ───────────────────────→│  Payment        │
│  (商品/訂單)     │                          │  (支付)         │
│                 │                          │                 │
│ Aggregate:      │  PaymentCompletedEvent  │ Aggregate:      │
│   Order         │ ←─────── ✕ ─────────────│   Payment       │
│   Product       │ (不會,Payment → Inventory) │ + IdempotencyKey│
└─────────────────┘                          └─────────────────┘
         ▲                                            │
         │                                            │
         │ InventoryDeductedEvent                     │ PaymentCompletedEvent
         │ (扣完通知商品標 COMPLETED)                   │
         │                                            ▼
         │                                  ┌─────────────────┐
         └──────────────────────────────────│  Inventory      │
                                            │  (庫存)         │
                                            │                 │
                                            │ Aggregate:      │
                                            │   Stock         │
                                            │   Reservation   │
                                            └─────────────────┘
```

三個 Bounded Context 對應三個頂層 package、三個 PostgreSQL schema:

```
com.tutorial.ecommerce.product.*    →  schema "product"
com.tutorial.ecommerce.payment.*    →  schema "payment"
com.tutorial.ecommerce.inventory.*  →  schema "inventory"
```

### 3.3 邊界規則(讓 Context 真的有邊界)

```
✅ 允許
  - 用 ApplicationEventPublisher 發送 Integration Event
  - 透過 Shared Kernel 共用 Value Object (Money, ProductId, OrderId 等)
  - 透過 Shared Kernel 共用 Integration Event 定義

❌ 禁止
  - Product 直接 import Payment 的 Domain Model
  - Product 直接存取 Payment schema 的 DB Table
  - 跨 Context 開同一個 @Transactional(就算進程內也不行)
```

**這些規則不是靠人盯,是靠 Spring Modulith + ArchUnit 自動把關**:

- `app/src/test/java/com/tutorial/ecommerce/modulith/ModulithStructureTest.java` — `ApplicationModules.verify()` 啟動時掃所有模組依賴方向
- `app/src/test/java/com/tutorial/ecommerce/architecture/LayerArchitectureTest.java` — 16 條 ArchUnit 規則(分層、Domain 不依賴 Spring、Adapter 不互相 import 等)

寫錯方向,測試直接紅,CI 擋下來。

### 3.4 Ubiquitous Language(在地語言)

DDD 強調**程式碼裡的詞要跟業務語言一致**。本專案的實際例子:

```java
// product 區段:Order 有狀態機,用 sealed interface 表達
public sealed interface OrderStatus
    permits Created, Paid, Completed, Cancelled, Refunded {

    record Created(Instant at) implements OrderStatus {}
    record Paid(Instant at, PaymentId paymentId) implements OrderStatus {}
    record Completed(Instant at) implements OrderStatus {}
    ...
}

// payment 區段:Payment 也有狀態機,但語言不一樣
public sealed interface PaymentStatus
    permits Pending, Completed, Failed, Refunded {
    ...
}
```

- Product 的「Completed」= 整個訂單流程跑完
- Payment 的「Completed」= 這筆款扣成功了

**同一個英文字,不同 context 意思不同 — 正是 Bounded Context 在做的事**。如果硬要共用一個全域 `Status` enum,反而把語言搞糊塗了。

### 3.5 Shared Kernel:哪些東西可以共用

兩個 context 之間真的有重疊時,用 **Shared Kernel**(共用核)放共同的概念。本專案的 Shared Kernel 只放三類:

```
shared-kernel/src/main/java/com/tutorial/ecommerce/sharedkernel/
├─ domain/    純 Value Object:Money, Quantity, OrderId, PaymentId, ProductId, UserId
├─ event/     Integration Event 契約:OrderCreatedEvent, PaymentCompletedEvent, ...
└─ port/      真的跨 context 共用的能力:ObjectStoragePort, SecretProvider
```

**規則**:
- Shared Kernel 不依賴任何業務 context(避免循環)
- 不依賴 Spring Framework(純 Java,讓所有 context 都能放心用)
- 標 `@ApplicationModule(type = OPEN)` 告訴 Spring Modulith「所有 sub-package 都可公開引用」

任何要放進 Shared Kernel 的東西,要過「**真的所有 context 都會用嗎?**」這道檻,否則寧可在每個 context 重寫一份。

---

## 4. 核心概念二:Event Bus 跨 Context 通訊

### 4.1 為什麼用事件,不用直接呼叫

假設 Product Context 收到下單請求,「直覺寫法」是這樣:

```java
// ❌ 反例:直接呼叫
@Service
class PlaceOrderHandler {
    private final PaymentService paymentService;     // ← Product 看得到 Payment 內部
    private final InventoryService inventoryService; // ← Product 看得到 Inventory 內部

    @Transactional   // ← 同一個交易跨三個 context
    void handle(PlaceOrderCommand cmd) {
        var order = orderRepo.save(...);
        paymentService.charge(order);    // 同步,炸了整個交易回滾
        inventoryService.deduct(order);  // 同步,炸了整個交易回滾
    }
}
```

**問題**:
1. Product 被迫知道 Payment 與 Inventory 的內部
2. 同一個交易跨三個 context → 一個區段 DB 慢,整單卡住
3. 改 Payment 內部會逼 Product 一起改

**正確寫法 — 事件驅動**:

```java
// ✅ Product 發事件,自己交易先 commit,完全不知道誰會接
@Service
class PlaceOrderHandler {
    @Transactional
    OrderId handle(PlaceOrderCommand cmd) {
        var order = Order.create(...);
        orderRepo.save(order);
        events.publishEvent(new OrderCreatedEvent(order.id(), ...));
        return order.id();
    }
}

// Payment 在另一個交易處理
@Component
class OrderCreatedEventListener {
    @ApplicationModuleListener           // ← AFTER_COMMIT + 新交易 + 失敗會重發
    void handle(OrderCreatedEvent e) {
        processPayment.process(...);
    }
}
```

Product 只知道「我發了一個 OrderCreatedEvent,有誰要接是別人的事」。

### 4.2 `@ApplicationModuleListener` 拆解

這是 Spring Modulith 提供的事件監聽註解,等同於以下四件事的組合:

```
@ApplicationModuleListener  =  @TransactionalEventListener(phase = AFTER_COMMIT)
                            +  @Async
                            +  @Transactional(propagation = REQUIRES_NEW)
                            +  Event Publication Log 寫入(失敗重發)
```

| 元素 | 作用 |
|---|---|
| `AFTER_COMMIT` | 發送端的 DB 交易 commit 成功才會觸發 listener,確保「事件代表已發生的事實」 |
| `@Async` | listener 在另一條 thread 跑,發送端的 API response 不被阻塞 |
| `REQUIRES_NEW` | listener 自己開新交易,跟發送端的交易完全脫鉤 |
| Event Publication Log | 事件寫到 `event_publication` table,listener 成功才標完成;失敗或重啟會自動重發 → **at-least-once 保證** |

### 4.3 事件流的真實樣貌

下一張流程圖比文字清楚:

```
時間軸 →

[Web 收到 POST /api/orders]
  ↓
[Product 模組]
  TX-1 開始
    └→ Order.create() → orderRepo.save() → publishEvent(OrderCreatedEvent)  ← 此時事件還沒真的發出去
  TX-1 commit
    └→ event_publication 表 INSERT OrderCreatedEvent (completion_date = NULL)
    └→ @Async 另起 thread 觸發 listener
  ↑ HTTP 201 回給呼叫端(到此為止,使用者看到的「下單成功」)

[Payment 模組,獨立 thread + 獨立交易]
  TX-2 開始
    └→ OrderCreatedEventListener.handle()
    └→ Payment.initiate() → Vault 取 API key → 模擬呼叫第三方扣款
    └→ MinIO 上傳收據
    └→ payment_repo.save() → publishEvent(PaymentCompletedEvent)
  TX-2 commit
    └→ event_publication 標 OrderCreatedEvent 完成
    └→ event_publication 表 INSERT PaymentCompletedEvent

[Inventory 模組,再獨立 thread + 獨立交易]
  TX-3 開始
    └→ PaymentCompletedEventListener.handle()
    └→ Redis 分散式鎖 → Stock.reserve() → 更新 stocks 表
    └→ publishEvent(InventoryDeductedEvent)
  TX-3 commit

[Product 模組,收回自己發起的事件鏈尾端]
  TX-4 開始
    └→ Order.markCompleted()
  TX-4 commit
```

**每一段都是獨立交易**,任何一段失敗都不會把其他段拖下水。失敗的事件還留在 `event_publication` 表,下次啟動或排程器會重發。

### 4.4 Saga 補償:失敗時怎麼回滾

事件鏈中段失敗(例如庫存不足),需要往回補償(退款)。這個專案的 Saga:

```
正常路徑:                              庫存不足的補償路徑:

OrderCreatedEvent                     OrderCreatedEvent
   ↓                                     ↓
PaymentCompletedEvent                 PaymentCompletedEvent
   ↓                                     ↓
InventoryDeductedEvent                InventoryDeductionFailedEvent  ← 庫存模組發出失敗事件
   ↓                                     ↓
Order → COMPLETED                     Payment.refund()             ← 支付模組接到,執行退款
                                         ↓
                                      Payment → REFUNDED
```

實作位置:`payment/src/main/java/.../application/eventlistener/InventoryDeductionFailedEventListener.java`

**重點**:補償也是「另一個事件」,不是 try/catch。寫法上完全對稱,失敗路徑跟成功路徑用同一套機制,只是事件型別不同。

### 4.5 怎麼測事件鏈

寫測試時不能 mock 掉 Event Bus,要驗證:
1. 發送端真的 publishEvent
2. AFTER_COMMIT 真的等 commit 才觸發
3. listener 真的接到
4. 鏈尾的狀態變化最終發生

用 Spring Modulith 的測試套件 + Awaitility:

```java
@SpringBootTest
@ContextConfiguration(initializers = SharedContainersInitializer.class)
class PlaceOrderE2EIT {
    @Test
    void placeOrder_endsWithInventoryDeducted() {
        // 1. POST /api/orders
        var resp = http.exchange("/api/orders", POST, ...);

        // 2. 不能立刻 assert,因為事件是 async
        // 用 Awaitility 輪詢,最多等 30 秒
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var stock = inventoryRepo.findByProductId(productId).orElseThrow();
            assertThat(stock.available().value()).isEqualTo(8);
        });
    }
}
```

實際檔案:`app/src/test/java/com/tutorial/ecommerce/e2e/PlaceOrderE2EIT.java`、`InventoryShortageSagaIT.java`

---

## 5. 核心概念三:Testcontainers 測試技巧

### 5.1 為什麼整合測試非得用容器

過去寫測試的兩個爛選擇:

```
A. 全 mock                          B. 開一台共用 dev DB
   ✗ 跟 prod 行為不一致               ✗ 慢、互相干擾、CI 排隊
   ✗ 改 schema mock 不會抗議          ✗ schema migration 沒驗證
   ✗ SQL 寫錯不會發現                 ✗ 測完留垃圾要清
```

Testcontainers 給你第三條路:**每次測試就地拉一個 Docker container,測完自動清掉**。可以用真的 PostgreSQL 跑 SQL、真的 Redis 測 expire、真的 Elasticsearch 測全文檢索 — 行為跟 prod 一致。

### 5.2 技巧一:Per-Adapter Integration Test

最容易上手的模式 — **每個 Adapter 配對一個 Container,單獨驗證**。

```java
@DataJpaTest                            // 只啟動 JPA slice,快
@AutoConfigureTestDatabase(replace = NONE)  // 不要用 H2 取代,要用真的
@Testcontainers                          // 啟用容器生命週期管理
@Import(JpaOrderRepository.class)       // 把我的 Adapter 拉進來
class JpaOrderRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JpaOrderRepository orderRepo;

    @Test
    void saveAndLoadOrder_preservesLinesAndStatus() {
        var order = Order.create(...);
        orderRepo.save(order);

        var loaded = orderRepo.findById(order.id()).orElseThrow();
        assertThat(loaded.lines()).hasSize(1);
        assertThat(loaded.status()).isInstanceOf(OrderStatus.Created.class);
    }
}
```

實際範例:
- `product/src/test/java/.../adapter/persistence/JpaOrderRepositoryIT.java` — PG
- `product/src/test/java/.../adapter/cache/RedisCacheAdapterIT.java` — Redis
- `product/src/test/java/.../adapter/search/ElasticsearchSearchAdapterIT.java` — ES
- `product/src/test/java/.../adapter/storage/MinioObjectStorageAdapterIT.java` — MinIO

每支測試只起 1-2 個容器,跑得快,訊號清楚 — 出錯時知道是 PG 還是 Redis 在抱怨。

### 5.3 技巧二:SingletonContainers + ApplicationContextInitializer(給 E2E 用)

E2E 測試要 6 個容器全開,如果每支測試都自己啟動會慢死。模式:**全 JVM 共用一份 Singleton Container**。

```java
public final class SharedContainers {

    public static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");
    public static final GenericContainer<?> REDIS = ...;
    public static final ElasticsearchContainer ES = ...;
    public static final GenericContainer<?> MINIO = ...;
    public static final VaultContainer<?> VAULT = ...;
    public static final KeycloakContainer KEYCLOAK = ...;

    private static volatile boolean started = false;

    public static synchronized void startAll() {
        if (started) return;
        POSTGRES.start();
        REDIS.start();
        // ...
        started = true;  // JVM 結束時 Testcontainers 自動 stop
    }
}
```

然後寫一個 `ApplicationContextInitializer` 把容器資訊塞進 Spring 環境變數:

```java
public class SharedContainersInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        SharedContainers.startAll();
        TestPropertyValues.of(
            "spring.datasource.url=" + SharedContainers.POSTGRES.getJdbcUrl(),
            "spring.data.redis.host=" + SharedContainers.REDIS.getHost(),
            "spring.data.redis.port=" + SharedContainers.REDIS.getFirstMappedPort(),
            "spring.elasticsearch.uris=http://" + SharedContainers.ES.getHttpHostAddress(),
            ...
        ).applyTo(ctx);
    }
}
```

每支 E2E 測試只要掛這個 initializer:

```java
@SpringBootTest(classes = ECommerceApplication.class, webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = SharedContainersInitializer.class)
@ActiveProfiles("test-real")
class PlaceOrderE2EIT { ... }
```

**好處**:
- 全 JVM 只啟動容器一次,即使有 10 支 E2E 也只等一次冷啟動
- Spring Context 還是會依測試類複用(同樣的 @ContextConfiguration 共用 cache)
- JVM 結束時 Testcontainers 用 Ryuk 自動清掉所有容器

實際檔案:`app/src/test/java/com/tutorial/ecommerce/e2e/SharedContainers.java` + `SharedContainersInitializer.java`

### 5.4 技巧三:`@DynamicPropertySource` vs Initializer 怎麼選

兩個機制都是「把 container 起來後的動態資訊塞進 Spring 環境變數」,但時機跟適用範圍不一樣:

| | `@DynamicPropertySource` | `ApplicationContextInitializer` |
|---|---|---|
| 觸發時機 | 容器啟動後、Spring 環境準備時 | Spring Context 完全建立前(更早) |
| 範圍 | 標在某支測試類,只給那支用 | 多個測試類可以共用同一個 initializer |
| 適用情境 | Per-adapter IT,單測試類 | 整個 E2E 系列共用同一組容器 |
| 語法 | `static void` 方法 + `DynamicPropertyRegistry` 參數 | 實作 `ApplicationContextInitializer` 介面 |

**選擇法則**:單測試類用 `@DynamicPropertySource`(簡單);多測試類共用 → Initializer。

### 5.5 技巧四:Per-Schema Flyway Migration

本專案三個 schema(product / payment / inventory),migration SQL 跟著各自模組走:

```
product/src/main/resources/db/migration/product/
  V001__create_schema.sql
  V002__create_orders_and_products.sql

payment/src/main/resources/db/migration/payment/
  V001__create_schema.sql
  V002__create_payments.sql

inventory/src/main/resources/db/migration/inventory/
  V001__create_schema.sql
  V002__create_stocks.sql
```

測試時可以**只載自己 schema 的 migration**,讓測試保持 isolated:

```java
@DynamicPropertySource
static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.schemas", () -> "product");
    registry.add("spring.flyway.default-schema", () -> "product");
    registry.add("spring.flyway.locations", () -> "classpath:db/migration/product");
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> "product");
}
```

E2E 測試則三 schema 全載,模擬 prod 狀況。

### 5.6 技巧五:把 IT 測試從 `./gradlew test` 隔離出去

Domain 跟 Application 單元測試要快、要每次都跑;Testcontainers IT 要 Docker、要慢、需要時才跑。我們在 `build.gradle.kts` 拆開:

```kotlin
tasks.named<Test>("test") {
    exclude("**/*IT.class")   // 預設 test 不跑 IT
    exclude("**/bdd/**")
}

val integrationTest = tasks.register<Test>("integrationTest") {
    include("**/*IT.class")    // 只跑 IT
    shouldRunAfter("test")
}
tasks.named("check") { dependsOn(integrationTest) }   // ./gradlew check 兩個都跑
```

跑法:
- `./gradlew test` — 純單元 + ArchUnit,30 秒,不用 Docker
- `./gradlew integrationTest` — 含 Docker 的 IT
- `./gradlew check` — 兩個都跑

### 5.7 技巧六:可切換 Adapter,Real vs Fake

本專案每個 Outbound Port 都有兩到三個 Adapter 實作:

| Port | Real(IT 用) | InMemory Fake(unit test 用) | 替代 Adapter |
|---|---|---|---|
| `OrderWriteRepository` | `JpaOrderRepository` | `InMemoryOrderRepository` | — |
| `SearchPort` | `ElasticsearchSearchAdapter` | `InMemorySearchAdapter` | — |
| `CachePort` | `RedisCacheAdapter` | `InMemoryCacheAdapter` | — |
| `ObjectStoragePort` | `MinioObjectStorageAdapter` | `InMemoryObjectStorage` | — |
| `SecretProvider` | `VaultSecretProvider` | — | `PropertyFileSecretProvider` |
| `DistributedLockPort` | `RedisDistributedLockAdapter` | — | `ReentrantLockAdapter` |

切換用 `@ConditionalOnProperty` + Profile,看 `application-test-real.yml` 與 `application-test-lite.yml`。

**重點**:Real adapter 跟 Fake 要 **行為一致**,否則「unit test 綠、IT 紅」會浪費你很多時間。用 **Port Contract Test** 確保:

```java
// product/src/test/java/.../contract/OrderRepositoryContract.java
abstract class OrderRepositoryContract {
    protected abstract OrderWriteRepository repository();

    @Test void saveThenFindById_roundTrip() { ... }
    @Test void save_updatesStatusOnSubsequentSave() { ... }
    // 同樣的斷言,InMemory 跟 JPA 各跑一次
}
```

### 5.8 何時不該用 Testcontainers

Testcontainers 很好用,但也不是萬靈丹。**不要拿來測純業務規則** — Domain 層用 InMemory + 純 JUnit 就好,沒理由為了測「訂單金額計算」啟動 PostgreSQL。

原則:**容器只用來測「跟外部系統互動」這件事本身**。

---

## 6. 環境準備與五分鐘上手

### 必裝

| 工具 | 版本 | 用途 |
|---|---|---|
| **JDK 21** | 21+ | 編譯與執行(用了 record / sealed interface) |
| **Gradle wrapper** | 內附 | 跑 `./gradlew` 即可 |

### 跑 IT / 本機完整啟動才需要

| 工具 | 用途 |
|---|---|
| **Docker** | Testcontainers / docker-compose |
| **Docker Compose v2** | 本機起 6 個服務 |
| **curl** | Vault bootstrap 腳本 |

### 三個 step 跑起來

```bash
# Step 1 — 只跑單元測試(30 秒,不用 Docker)
./gradlew test

# Step 2 — 跑容器整合測試(需要 Docker)
./gradlew integrationTest

# Step 3 — 本機完整啟動 + 打 API
docker compose up -d
./scripts/vault-bootstrap.sh
./gradlew :app:bootRun
```

打 API 拿 token + 下單:

```bash
TOKEN=$(curl -sS -X POST \
  http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'grant_type=password&client_id=ecommerce-web&username=buyer01&password=secret' \
  | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/products?q=iPhone"
```

### 看模組依賴圖

```bash
./gradlew :app:syncModulithDocs
```

`docs/spring-modulith/components.puml` 是 Spring Modulith 自動產的 C4 圖,貼進 [PlantUML Server](https://www.plantuml.com/plantuml) 即可視覺化看到三個 Context 之間的事件依賴。

---

## 7. 專案結構導覽

```
monolithic-app-with-testcontainers-tutorial-for-java/   (repo root)
├─ README.md             你正在讀的這份
├─ build.gradle.kts      根 build,所有子模組共用設定
├─ settings.gradle.kts   宣告 6 個子模組
├─ gradle/libs.versions.toml  版本目錄
├─ gradlew, gradlew.bat  Gradle wrapper
│
├─ shared-kernel/        三個 Context 共用的最小集合(VO + Event + 通用 Port)
│
├─ product/              Bounded Context #1:商品 + 訂單
│  └─ src/main/java/com/tutorial/ecommerce/product/
│     ├─ domain/         純 Java,業務規則(Order, Product, OrderStatus, PricingService)
│     ├─ application/    @Service handlers + Event Listeners
│     └─ adapter/        REST controller、JPA、ES、Redis、MinIO
│
├─ payment/              Bounded Context #2:支付(同樣結構)
│
├─ inventory/            Bounded Context #3:庫存(同樣結構)
│
├─ infrastructure/       全域基礎設施(Security、共用 DataSource)
│                        — 注意:這不是業務 Context,沒標 @ApplicationModule
│
├─ app/                  啟動模組
│  ├─ src/main/java/com/tutorial/ecommerce/ECommerceApplication.java   @Modulithic
│  └─ src/test/java/com/tutorial/ecommerce/
│     ├─ architecture/   ArchUnit 16 條規則(分層、Context 邊界、命名)
│     ├─ modulith/       Spring Modulith 自動驗證 + Documenter
│     ├─ e2e/            6 容器 SharedContainers + E2E + Saga 測試
│     └─ bdd/            Cucumber 中文 Gherkin
│
├─ docker-compose.yml    本機開發起 6 個服務
├─ scripts/vault-bootstrap.sh  種 Vault secret
└─ docs/
   ├─ design/            原始設計文件 (testcontainers-monolith-java.md)
   └─ spring-modulith/   自動產的模組關係 PlantUML / AsciiDoc
```

每個業務 Context 內部都是 **Hexagonal 三層**(domain → application → adapter),這是落實 DDD Bounded Context 的常見搭配。Hexagonal 不是必要,但它讓「禁止 Domain 依賴 Spring」這類規則容易自動驗證。

---

## 8. 測試策略

```
                        ╱╲
                       ╱  ╲          E2E IT (需 Docker)
                      ╱ 慢 ╲          6 容器 + 全鏈路事件 + Awaitility
                     ╱──────╲        驗證 Bounded Context 之間的事件鏈
                    ╱        ╲
                   ╱ Adapter ╲       Per-Adapter IT (需 Docker, 跑很多)
                  ╱   IT      ╲      每個 Adapter 對 1-2 個 Container
                 ╱──────────────╲    驗證 SQL / Redis script / ES query 是真的對的
                ╱                ╲
               ╱  Application     ╲  CommandHandler + InMemory Fake (毫秒)
              ╱  Unit Test         ╲  驗證 Application 串接邏輯
             ╱──────────────────────╲
            ╱   Domain Unit Test     ╲ 純 Java,<1ms/test
           ╱  (沒有 Spring,沒有容器)  ╲ 驗證業務規則(訂單狀態機、金額計算...)
          ╱──────────────────────────╲
         ╱   ArchUnit + Modulith      ╲ Bounded Context 邊界守門員
        ╱   (沒有業務行為,只看依賴)     ╲ Domain 不依賴 Spring,Context 之間不互 import
       ╱────────────────────────────────╲
```

**金字塔下層越厚越好**(快、穩、訊號清楚)。上層只挑代表性 case,不要全鏈路測一切。

---

## 9. 常見問題與排錯

### Q1. `./gradlew test` 紅了,說 Docker 連不上

預設 `test` task 已排除 `*IT.class`。如果還是看到 Docker 錯誤,代表某支 `*Test.java` 不小心加了 `@Testcontainers`。回頭把 Testcontainers 相關的測試類改名為 `*IT.java`,或 `build.gradle.kts:70-73` 加進 exclude 清單。

### Q2. ArchUnit 一直報「empty should」

預設規則對空 package 也會報錯,新加的 package 還沒類別時會誤報。已在 `app/src/test/resources/archunit.properties` 設 `archRule.failOnEmptyShould=false`,LayeredArchitecture 加 `.withOptionalLayers(true)`。新增業務類別後規則會自動生效。

### Q3. Modulith 報「Module X depends on non-exposed type from Y」

代表 X 引用了 Y 的內部型別。兩種解法:
- 真的需要跨用 → Y 在那個 sub-package 標 `@org.springframework.modulith.NamedInterface`
- 或者那個型別本來就該放 Shared Kernel

Shared Kernel 本身標了 `type = OPEN`,所有 sub-package 對外公開,不需要再標。

### Q4. Spring Boot 4 跟 3 套件位置差很多

實際對照表(從 3.4 升到 4.1 踩過的):

| 3.x | 4.x |
|---|---|
| `org.springframework.boot.autoconfigure.domain.EntityScan` | `org.springframework.boot.persistence.autoconfigure.EntityScan` |
| `org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| `org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase` | `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase` |
| `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `org.springframework.boot.test.web.client.TestRestTemplate` | `org.springframework.boot.resttestclient.TestRestTemplate` |

對應的 artifact 也拆細了,`gradle/libs.versions.toml` 內有完整列表。

### Q5. 容器啟動很慢

Elasticsearch 首次啟動約 30-60 秒、Keycloak 約 20 秒,屬正常。要加速:

```bash
# 啟用容器重用(同樣的容器設定下次跑會復用)
echo "testcontainers.reuse.enable=true" >> ~/.testcontainers.properties
```

注意:重用之後容器不會被清掉,需要手動 `docker container prune`。

### Q6. 我想新增第四個 Bounded Context(例如 Notification)

1. `settings.gradle.kts` 加 `include("notification")`
2. 建 `notification/build.gradle.kts`(複製 inventory 的當範本)
3. `notification/src/main/java/.../notification/package-info.java` 標 `@ApplicationModule(displayName="Notification")`
4. 想接事件 → 寫 `@ApplicationModuleListener`
5. 想發事件 → 在 `shared-kernel/event/` 新增 record
6. 跑 `./gradlew :app:test` — Modulith 與 ArchUnit 會自動把它納入驗證

---

## 10. 延伸學習

### 把 DDD 推得更深

- **Event Storming workshop**:跟 PM/業務寫白板,用 Domain Event 找出 Bounded Context(這個專案是「結果」,Event Storming 是「過程」)
- **Context Mapping**:三個 Context 之間是 Customer-Supplier、Conformist 還是 Open Host Service?本專案是 Open Host Service + Published Language(事件就是契約)
- **Aggregate 設計**:`Order` 為什麼包含 `OrderLine`,但 `Product` 不包含?這背後是「Aggregate 邊界 = 交易邊界 = 一致性邊界」的取捨

推薦讀物:Evans《Domain-Driven Design》、Vernon《Implementing DDD》、Khononov《Learning DDD》

### 把事件機制推得更深

- 看 `event_publication` table:`SELECT * FROM event_publication WHERE completion_date IS NULL;` — 失敗的事件還躺在這
- 試著手動 kill 掉支付過程中的 process,重啟後看事件會不會自動重發
- 把 `@ApplicationModuleListener` 換成 `@TransactionalEventListener` 拆開,觀察各個元素的單獨效果
- 進階:Outbox Pattern 的不同實作(polling、CDC、寫多份)

### 把 Testcontainers 推得更深

- 試 Testcontainers Cloud(SaaS 容器)讓 CI 跑得更快
- 寫自己的 `GenericContainer` 包裝(例如包一個 Kafka)
- Container Network:多個容器組合(Postgres + Debezium + Kafka)模擬複雜架構
- Reusable Containers + Test Lifecycle:同一組容器跨多個 test class 重用

### 真的拆成微服務?

本專案故意把「拆得動」當成設計目標的一部分。三 schema 隔離、無跨 context 交易、Port/Adapter — 這些都讓未來拆分時改動最小。但**拆與不拆是業務問題,不是技術問題**。先問:「現在的痛是不是只有微服務能解?」

---

## 附錄 A — 常用指令速查

```bash
# 測試
./gradlew test                            # 全部單元測試 + ArchUnit (~30 秒)
./gradlew :product:test                   # 只跑 product context
./gradlew :product:test --tests "OrderTest"
./gradlew integrationTest                 # 含 Docker 的 IT
./gradlew check                           # test + integrationTest

# 建置
./gradlew :app:bootJar                    # 打包成可執行 jar
./gradlew :app:bootRun                    # 直接跑

# 文件
./gradlew :app:syncModulithDocs           # 把模組關係圖搬到 docs/

# 維護
./gradlew dependencies                    # 看依賴樹
./gradlew --refresh-dependencies          # 強制重抓
./gradlew clean                           # 清掉 build/
```

## 附錄 B — Profile 切換對照

| Profile | DB | Cache | Search | Storage | Secret | Lock |
|---|---|---|---|---|---|---|
| (預設) | PostgreSQL | Redis | Elasticsearch | MinIO | Vault | Redis |
| `test-real` | PG container | Redis container | ES container | MinIO container | Vault container | Redis container |
| `test-lite` | PG container | InMemory | InMemory | InMemory | PropertyFile | Reentrant |

`test-lite` 給「我不想開那麼多容器」時用,InMemory adapter 取代 ES/Redis/MinIO/Vault。

---

**Happy learning!**

把這份專案 clone 下來,從 `product/src/main/java/.../domain/model/Order.java` 開始讀,接著看 `application/command/PlaceOrderCommandHandler.java` 怎麼發事件,再看 `payment/.../application/eventlistener/OrderCreatedEventListener.java` 怎麼接 — 把整條鏈走過一遍,DDD + Event Bus + Testcontainers 三件事就接起來了。
