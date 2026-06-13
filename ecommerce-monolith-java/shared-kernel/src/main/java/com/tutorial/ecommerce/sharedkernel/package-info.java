/**
 * Shared Kernel — 跨模組共用的最小集合:
 *   - Value Object (Money, Quantity, ProductId, OrderId, PaymentId, UserId)
 *   - Integration Event records (跨模組通訊契約)
 *   - 通用 Outbound Port (ObjectStoragePort, SecretProvider)
 *
 * 規則:
 *   - 不得依賴任何業務模組
 *   - 不得依賴 Spring Framework (純 Java)
 *   - 任何放這裡的型別,所有模組都看得到,所以只放真正共用的東西
 *
 * 標 type = OPEN — Spring Modulith 視為 shared kernel,允許其他模組 import 任何 sub-package。
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Shared Kernel",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.tutorial.ecommerce.sharedkernel;
