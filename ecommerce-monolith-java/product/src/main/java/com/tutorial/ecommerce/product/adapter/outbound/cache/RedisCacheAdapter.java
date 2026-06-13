package com.tutorial.ecommerce.product.adapter.outbound.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.ecommerce.product.domain.port.outbound.CachePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "ecommerce.adapter.cache", havingValue = "redis", matchIfMissing = true)
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisCacheAdapter(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        var raw = redis.opsForValue().get(key);
        if (raw == null) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(raw, type));
        } catch (Exception e) {
            redis.delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("redis cache put failed for key=" + key, e);
        }
    }

    @Override
    public void evict(String key) {
        redis.delete(key);
    }
}
