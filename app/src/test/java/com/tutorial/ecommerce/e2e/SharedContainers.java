package com.tutorial.ecommerce.e2e;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

/**
 * Singleton 容器集合 — 整個 test JVM 共用 6 個容器,避免每個測試各起一份。
 * JVM shutdown 時 Testcontainers 自動清理。
 */
public final class SharedContainers {

    public static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce")
            .withUsername("ecommerce")
            .withPassword("ecommerce");

    public static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    public static final ElasticsearchContainer ES =
        new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.0"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    public static final GenericContainer<?> MINIO =
        new GenericContainer<>("minio/minio:RELEASE.2024-08-17T01-24-54Z")
            .withCommand("server", "/data")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withExposedPorts(9000);

    public static final VaultContainer<?> VAULT =
        new VaultContainer<>("hashicorp/vault:1.17")
            .withVaultToken("dev-root-token");

    public static final KeycloakContainer KEYCLOAK =
        new KeycloakContainer("quay.io/keycloak/keycloak:25.0")
            .withRealmImportFile("keycloak/ecommerce-realm.json");

    private static volatile boolean started = false;

    private SharedContainers() {}

    /** 第一次呼叫時啟動所有容器,後續呼叫即刻返回。 */
    public static synchronized void startAll() {
        if (started) return;
        POSTGRES.start();
        REDIS.start();
        ES.start();
        MINIO.start();
        VAULT.start();
        KEYCLOAK.start();
        started = true;
    }
}
