package com.tutorial.ecommerce.product.adapter.search;

import com.tutorial.ecommerce.product.adapter.outbound.search.ElasticsearchSearchAdapter;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DataElasticsearchTest
@Testcontainers
@Import(ElasticsearchSearchAdapter.class)
class ElasticsearchSearchAdapterIT {

    @Container
    static final ElasticsearchContainer ES = new ElasticsearchContainer(
        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.0")
    ).withEnv("xpack.security.enabled", "false")
     .withEnv("discovery.type", "single-node");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + ES.getHttpHostAddress());
    }

    @Autowired ElasticsearchSearchAdapter adapter;

    @Test
    void indexAndSearch_returnsMatching() {
        var iphone = ProductId.newId();
        var pixel = ProductId.newId();
        adapter.index(iphone, "iPhone 16", "Apple flagship phone");
        adapter.index(pixel, "Pixel 9", "Google phone");

        await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ProductId> hits = adapter.search("phone", 10);
            assertThat(hits).hasSizeGreaterThanOrEqualTo(2);
        });
    }
}
