package com.tutorial.ecommerce.payment.adapter.outbound.storage;

import com.tutorial.ecommerce.sharedkernel.port.ObjectStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 支付模組專屬的 MinIO ObjectStorage adapter,bean name 與商品模組不同。
 * 兩個模組各自的 PostConstruct 確保各自的 bucket 存在。
 */
@Component("paymentReceiptStorage")
@ConditionalOnProperty(name = "ecommerce.payment.storage", havingValue = "minio", matchIfMissing = true)
public class PaymentReceiptStorageAdapter implements ObjectStoragePort {

    private final MinioClient client;
    private final String bucket;

    public PaymentReceiptStorageAdapter(
        MinioClient client,
        @Value("${ecommerce.payment.receipt-bucket:payment-receipts}") String bucket
    ) {
        this.client = client;
        this.bucket = bucket;
    }

    @PostConstruct
    void ensureBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("cannot ensure bucket " + bucket, e);
        }
    }

    @Override
    public void put(String bucketIgnored, String objectKey, InputStream content, long contentLength, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(objectKey)
                .stream(content, contentLength, -1)
                .contentType(contentType).build());
        } catch (Exception e) {
            throw new IllegalStateException("receipt put failed: " + objectKey, e);
        }
    }

    @Override
    public InputStream get(String bucketIgnored, String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            throw new IllegalStateException("receipt get failed", e);
        }
    }

    @Override
    public boolean exists(String bucketIgnored, String objectKey) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("receipt stat failed", e);
        }
    }

    @Override
    public String presignedGetUrl(String bucketIgnored, String objectKey, Duration ttl) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET).bucket(bucket).object(objectKey)
                .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS).build());
        } catch (Exception e) {
            throw new IllegalStateException("receipt presign failed", e);
        }
    }
}
