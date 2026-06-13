package com.tutorial.ecommerce.product.application.fake;

import com.tutorial.ecommerce.product.domain.port.outbound.CachePort;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCacheAdapter implements CachePort {

    private final Map<String, Object> store = new ConcurrentHashMap<>();
    public int hits;
    public int misses;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        var v = store.get(key);
        if (v == null) { misses++; return Optional.empty(); }
        hits++;
        return Optional.of(type.cast(v));
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        store.put(key, value);
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }
}
