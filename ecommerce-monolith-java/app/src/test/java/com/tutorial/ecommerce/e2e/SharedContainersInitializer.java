package com.tutorial.ecommerce.e2e;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 把 SharedContainers 注入 Spring 環境變數,避免每個測試類重複寫 @DynamicPropertySource。
 */
public class SharedContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        SharedContainers.startAll();
        TestPropertyValues.of(
            "spring.datasource.url=" + SharedContainers.POSTGRES.getJdbcUrl(),
            "spring.datasource.username=" + SharedContainers.POSTGRES.getUsername(),
            "spring.datasource.password=" + SharedContainers.POSTGRES.getPassword(),
            "spring.data.redis.host=" + SharedContainers.REDIS.getHost(),
            "spring.data.redis.port=" + SharedContainers.REDIS.getFirstMappedPort(),
            "spring.elasticsearch.uris=http://" + SharedContainers.ES.getHttpHostAddress(),
            "ecommerce.minio.endpoint=http://" + SharedContainers.MINIO.getHost() + ":" + SharedContainers.MINIO.getFirstMappedPort(),
            "ecommerce.minio.access-key=minioadmin",
            "ecommerce.minio.secret-key=minioadmin",
            "spring.cloud.vault.uri=" + SharedContainers.VAULT.getHttpHostAddress(),
            "spring.cloud.vault.token=dev-root-token",
            "spring.security.oauth2.resourceserver.jwt.issuer-uri="
                + SharedContainers.KEYCLOAK.getAuthServerUrl() + "/realms/ecommerce"
        ).applyTo(ctx);
    }
}
