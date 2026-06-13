package com.tutorial.ecommerce.product.adapter.storage;

import com.tutorial.ecommerce.product.adapter.outbound.storage.MinioObjectStorageAdapter;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MinioObjectStorageAdapterIT {

    static final String BUCKET = "product-images";

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("minio/minio:RELEASE.2024-08-17T01-24-54Z")
        .withCommand("server", "/data")
        .withEnv("MINIO_ROOT_USER", "minioadmin")
        .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
        .withExposedPorts(9000);

    static MinioClient client;
    static MinioObjectStorageAdapter adapter;

    @BeforeAll
    static void setUp() throws Exception {
        var endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getFirstMappedPort();
        client = MinioClient.builder()
            .endpoint(endpoint)
            .credentials("minioadmin", "minioadmin")
            .build();
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
        adapter = new MinioObjectStorageAdapter(client);
    }

    @Test
    void putExistsAndPresignedUrl_roundTrip() {
        byte[] content = "hello".getBytes();
        adapter.put(BUCKET, "test.txt", new ByteArrayInputStream(content), content.length, "text/plain");

        assertThat(adapter.exists(BUCKET, "test.txt")).isTrue();
        assertThat(adapter.exists(BUCKET, "missing.txt")).isFalse();
        var url = adapter.presignedGetUrl(BUCKET, "test.txt", Duration.ofMinutes(5));
        assertThat(url).contains("test.txt");
    }
}
