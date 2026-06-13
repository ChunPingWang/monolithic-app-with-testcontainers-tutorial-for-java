package com.tutorial.ecommerce.payment.application.fake;

import com.tutorial.ecommerce.sharedkernel.port.ObjectStoragePort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryObjectStorage implements ObjectStoragePort {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    private String key(String bucket, String key) { return bucket + ":" + key; }

    @Override
    public void put(String bucket, String key, InputStream content, long contentLength, String contentType) {
        try {
            store.put(key(bucket, key), content.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream get(String bucket, String key) {
        return new ByteArrayInputStream(store.get(key(bucket, key)));
    }

    @Override
    public boolean exists(String bucket, String key) {
        return store.containsKey(key(bucket, key));
    }

    @Override
    public String presignedGetUrl(String bucket, String key, Duration ttl) {
        return "memory://" + key(bucket, key);
    }
}
