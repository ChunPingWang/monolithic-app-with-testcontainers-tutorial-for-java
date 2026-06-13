package com.tutorial.ecommerce.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 集中掃描三個業務模組的 JPA entity / repository,但共用單一 DataSource。
 * 每個 Entity 必須在自己的 @Table(schema = "...") 標 schema,Flyway 用 schemas: product,payment,inventory 各自遷移。
 */
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.tutorial.ecommerce.product.adapter.outbound.persistence",
    "com.tutorial.ecommerce.payment.adapter.outbound.persistence",
    "com.tutorial.ecommerce.inventory.adapter.outbound.persistence"
})
@EntityScan(basePackages = {
    "com.tutorial.ecommerce.product.adapter.outbound.persistence",
    "com.tutorial.ecommerce.payment.adapter.outbound.persistence",
    "com.tutorial.ecommerce.inventory.adapter.outbound.persistence"
})
public class JpaSchemaIsolationConfig {

    @SuppressWarnings("unused")
    private EntityManagerFactory entityManagerFactory;
}
