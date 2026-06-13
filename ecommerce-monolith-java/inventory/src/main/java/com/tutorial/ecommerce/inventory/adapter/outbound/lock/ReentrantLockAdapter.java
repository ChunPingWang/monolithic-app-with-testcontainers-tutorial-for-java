package com.tutorial.ecommerce.inventory.adapter.outbound.lock;

import com.tutorial.ecommerce.inventory.domain.port.outbound.DistributedLockPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** 替代 Adapter — 單實例部署時的本地鎖,測試也可用。 */
@Component
@ConditionalOnProperty(name = "ecommerce.adapter.lock", havingValue = "reentrant")
public class ReentrantLockAdapter implements DistributedLockPort {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String key, Duration ttl, Supplier<T> action) {
        var l = locks.computeIfAbsent(key, k -> new ReentrantLock());
        l.lock();
        try {
            return action.get();
        } finally {
            l.unlock();
        }
    }
}
