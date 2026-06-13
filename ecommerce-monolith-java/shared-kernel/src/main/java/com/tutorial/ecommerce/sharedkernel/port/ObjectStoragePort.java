package com.tutorial.ecommerce.sharedkernel.port;

import java.io.InputStream;
import java.time.Duration;

/**
 * 物件儲存 Outbound Port — 多模組共用(商品圖、支付收據)。
 * Real adapter: MinIO / S3。Fake: InMemory。
 */
public interface ObjectStoragePort {

    void put(String bucket, String objectKey, InputStream content, long contentLength, String contentType);

    InputStream get(String bucket, String objectKey);

    boolean exists(String bucket, String objectKey);

    String presignedGetUrl(String bucket, String objectKey, Duration ttl);
}
