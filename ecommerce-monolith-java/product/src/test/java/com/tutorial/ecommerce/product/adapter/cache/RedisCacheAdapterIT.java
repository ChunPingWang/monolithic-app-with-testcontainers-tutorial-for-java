package com.tutorial.ecommerce.product.adapter.cache;

import com.tutorial.ecommerce.product.adapter.outbound.cache.RedisCacheAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Testcontainers
@Import(RedisCacheAdapter.class)
class RedisCacheAdapterIT {

    @Container
    static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    }

    @Autowired RedisCacheAdapter cache;

    record CacheValue(String name, int qty) {}

    @Test
    void putThenGet_roundTrip() {
        cache.put("k1", new CacheValue("iPhone", 3), Duration.ofMinutes(5));

        Optional<CacheValue> hit = cache.get("k1", CacheValue.class);

        assertThat(hit).isPresent();
        assertThat(hit.get().name()).isEqualTo("iPhone");
    }

    @Test
    void get_missingKey_returnsEmpty() {
        assertThat(cache.get("nope", CacheValue.class)).isEmpty();
    }

    @Test
    void evict_removesEntry() {
        cache.put("k2", new CacheValue("x", 1), Duration.ofMinutes(5));
        cache.evict("k2");

        assertThat(cache.get("k2", CacheValue.class)).isEmpty();
    }
}
