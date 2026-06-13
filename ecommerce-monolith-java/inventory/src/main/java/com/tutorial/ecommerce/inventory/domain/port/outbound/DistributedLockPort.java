package com.tutorial.ecommerce.inventory.domain.port.outbound;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分散式鎖 Port — 用於跨進程併發控制。
 * Real adapter: Redis SETNX。Fake: ReentrantLock。替代:JDBC SELECT FOR UPDATE。
 */
public interface DistributedLockPort {

    <T> T withLock(String key, Duration ttl, Supplier<T> action);
}
