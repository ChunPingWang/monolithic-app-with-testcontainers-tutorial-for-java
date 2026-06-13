package com.tutorial.ecommerce.payment.contract;

import com.tutorial.ecommerce.payment.adapter.outbound.persistence.JpaPaymentRepository;
import com.tutorial.ecommerce.payment.domain.port.outbound.PaymentWriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaPaymentRepository.class)
class JpaPaymentRepositoryContractIT extends PaymentRepositoryContract {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.schemas", () -> "payment");
        registry.add("spring.flyway.default-schema", () -> "payment");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/payment");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "payment");
    }

    @Autowired JpaPaymentRepository jpaPaymentRepository;

    @Override
    protected PaymentWriteRepository repository() { return jpaPaymentRepository; }
}
