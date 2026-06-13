package com.tutorial.ecommerce.product.adapter.outbound.storage;

import com.tutorial.ecommerce.sharedkernel.port.ObjectStoragePort;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "ecommerce.adapter.storage", havingValue = "minio", matchIfMissing = true)
public class MinioObjectStorageAdapter implements ObjectStoragePort {

    private final MinioClient client;

    public MinioObjectStorageAdapter(MinioClient client) {
        this.client = client;
    }

    @Override
    public void put(String bucket, String objectKey, InputStream content, long contentLength, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(content, contentLength, -1)
                .contentType(contentType)
                .build());
        } catch (Exception e) {
            throw new IllegalStateException("minio put failed: " + bucket + "/" + objectKey, e);
        }
    }

    @Override
    public InputStream get(String bucket, String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            throw new IllegalStateException("minio get failed: " + bucket + "/" + objectKey, e);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("minio stat failed", e);
        }
    }

    @Override
    public String presignedGetUrl(String bucket, String objectKey, Duration ttl) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectKey)
                .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                .build());
        } catch (Exception e) {
            throw new IllegalStateException("minio presign failed", e);
        }
    }
}
