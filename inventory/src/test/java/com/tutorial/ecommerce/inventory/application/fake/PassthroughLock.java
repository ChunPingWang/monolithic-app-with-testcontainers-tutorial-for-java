package com.tutorial.ecommerce.inventory.application.fake;

import com.tutorial.ecommerce.inventory.domain.port.outbound.DistributedLockPort;

import java.time.Duration;
import java.util.function.Supplier;

public class PassthroughLock implements DistributedLockPort {
    @Override
    public <T> T withLock(String key, Duration ttl, Supplier<T> action) {
        return action.get();
    }
}
