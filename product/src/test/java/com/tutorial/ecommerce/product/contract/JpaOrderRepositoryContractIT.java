package com.tutorial.ecommerce.product.contract;

import com.tutorial.ecommerce.product.adapter.outbound.persistence.JpaOrderRepository;
import com.tutorial.ecommerce.product.adapter.outbound.persistence.JpaProductRepository;
import com.tutorial.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Real Adapter 端的契約驗證。
 *
 * 跟 {@link InMemoryOrderRepositoryContractTest} 跑「完全同一份」測試,
 * 只是 repository() 換成 JPA 實作。任何 InMemory 跟 JPA 行為不一致的地方,
 * 同一條測試會在這裡紅、那裡綠 — 立刻暴露假象。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({JpaOrderRepository.class, JpaProductRepository.class})
class JpaOrderRepositoryContractIT extends OrderRepositoryContract {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.schemas", () -> "product");
        registry.add("spring.flyway.default-schema", () -> "product");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/product");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "product");
    }

    @Autowired JpaOrderRepository jpaOrderRepository;

    @Override
    protected OrderWriteRepository repository() { return jpaOrderRepository; }
}
