package com.tutorial.ecommerce.product.adapter.persistence;

import com.tutorial.ecommerce.product.adapter.outbound.persistence.JpaOrderRepository;
import com.tutorial.ecommerce.product.adapter.outbound.persistence.JpaProductRepository;
import com.tutorial.ecommerce.product.domain.model.Order;
import com.tutorial.ecommerce.product.domain.model.OrderLine;
import com.tutorial.ecommerce.product.domain.model.OrderStatus;
import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.PaymentId;
import com.tutorial.ecommerce.sharedkernel.domain.Quantity;
import com.tutorial.ecommerce.sharedkernel.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({JpaOrderRepository.class, JpaProductRepository.class})
class JpaOrderRepositoryIT {

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

    @Autowired JpaOrderRepository orderRepo;
    @Autowired JpaProductRepository productRepo;

    @Test
    void saveAndLoadProduct_roundTrip() {
        var p = Product.create("iPhone 16", "flagship", Money.of("30000.00", "TWD"));

        productRepo.save(p);
        var loaded = productRepo.findById(p.id()).orElseThrow();

        assertThat(loaded.name()).isEqualTo("iPhone 16");
        assertThat(loaded.price()).isEqualTo(Money.of("30000.00", "TWD"));
    }

    @Test
    void saveAndLoadOrder_preservesLinesAndStatus() {
        var product = Product.create("iPhone 16", "flagship", Money.of("30000.00", "TWD"));
        productRepo.save(product);
        var order = Order.create(
            new UserId("buyer01"),
            List.of(new OrderLine(product.id(), Quantity.of(2), Money.of("30000.00", "TWD")))
        );

        orderRepo.save(order);
        var loaded = orderRepo.findById(order.id()).orElseThrow();

        assertThat(loaded.lines()).hasSize(1);
        assertThat(loaded.totalAmount()).isEqualTo(Money.of("60000.00", "TWD"));
        assertThat(loaded.status()).isInstanceOf(OrderStatus.Created.class);
    }

    @Test
    void statusTransitions_persistAcrossSaves() {
        var product = Product.create("Pixel 9", "smartphone", Money.of("25000.00", "TWD"));
        productRepo.save(product);
        var order = Order.create(
            new UserId("buyer01"),
            List.of(new OrderLine(product.id(), Quantity.of(1), Money.of("25000.00", "TWD")))
        );
        orderRepo.save(order);

        var loaded = orderRepo.findById(order.id()).orElseThrow();
        loaded.markPaid(PaymentId.newId());
        orderRepo.save(loaded);

        var paid = orderRepo.findById(order.id()).orElseThrow();
        assertThat(paid.status()).isInstanceOf(OrderStatus.Paid.class);
    }
}
