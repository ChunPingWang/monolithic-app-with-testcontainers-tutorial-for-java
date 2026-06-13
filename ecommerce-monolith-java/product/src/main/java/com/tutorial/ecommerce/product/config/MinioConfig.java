package com.tutorial.ecommerce.product.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    @ConfigurationProperties("ecommerce.minio")
    MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean
    MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
            .endpoint(props.getEndpoint())
            .credentials(props.getAccessKey(), props.getSecretKey())
            .build();
    }

    public static class MinioProperties {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    }
}
