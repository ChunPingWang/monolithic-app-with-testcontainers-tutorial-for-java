/**
 * 全域基礎設施 — 不屬於任何業務模組,提供共用設定:
 *   - auth         OAuth2 Resource Server (Keycloak)
 *   - persistence  DataSource、JPA、Flyway 多 schema
 *   - observability Actuator、Micrometer、結構化日誌
 *
 * 此 package 不標 @ApplicationModule — 它是平台層,不是業務模組。
 */
package com.tutorial.ecommerce.infrastructure;
