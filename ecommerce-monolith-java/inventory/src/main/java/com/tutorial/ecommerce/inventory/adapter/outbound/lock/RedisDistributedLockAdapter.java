package com.tutorial.ecommerce.inventory.adapter.outbound.lock;

import com.tutorial.ecommerce.inventory.domain.port.outbound.DistributedLockPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SET key value NX PX <ttl> — 經典 Redis 分散式鎖。
 * 取得失敗會 busy-wait 短暫後重試,最多嘗試 50 次。
 * 釋放鎖時用 Lua script CAS,避免誤刪別人的鎖。
 */
@Component
@ConditionalOnProperty(name = "ecommerce.adapter.lock", havingValue = "redis", matchIfMissing = true)
public class RedisDistributedLockAdapter implements DistributedLockPort {

    private static final String UNLOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final StringRedisTemplate redis;

    public RedisDistributedLockAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public <T> T withLock(String key, Duration ttl, Supplier<T> action) {
        var token = UUID.randomUUID().toString();
        if (!acquire(key, token, ttl)) {
            throw new IllegalStateException("could not acquire lock: " + key);
        }
        try {
            return action.get();
        } finally {
            redis.execute(
                org.springframework.data.redis.core.script.RedisScript.of(UNLOCK_SCRIPT, Long.class),
                java.util.List.of(key),
                token
            );
        }
    }

    private boolean acquire(String key, String token, Duration ttl) {
        for (int i = 0; i < 50; i++) {
            Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
            if (Boolean.TRUE.equals(ok)) return true;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
