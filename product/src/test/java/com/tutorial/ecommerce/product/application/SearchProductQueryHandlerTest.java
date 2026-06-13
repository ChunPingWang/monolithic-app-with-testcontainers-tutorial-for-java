package com.tutorial.ecommerce.product.application;

import com.tutorial.ecommerce.product.application.fake.InMemoryCacheAdapter;
import com.tutorial.ecommerce.product.application.fake.InMemoryProductRepository;
import com.tutorial.ecommerce.product.application.fake.InMemorySearchAdapter;
import com.tutorial.ecommerce.product.application.query.SearchProductQueryHandler;
import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchProductQueryHandlerTest {

    private InMemoryProductRepository repo;
    private InMemorySearchAdapter search;
    private InMemoryCacheAdapter cache;
    private SearchProductQueryHandler handler;

    @BeforeEach
    void setUp() {
        repo = new InMemoryProductRepository();
        search = new InMemorySearchAdapter();
        cache = new InMemoryCacheAdapter();
        handler = new SearchProductQueryHandler(repo, search, cache);
    }

    @Test
    void findById_secondCallHitsCache() {
        var p = Product.create("iPhone 16", "flagship phone", Money.of("30000.00", "TWD"));
        repo.save(p);

        handler.findById(p.id());
        handler.findById(p.id());

        assertThat(cache.hits).isEqualTo(1);
        assertThat(cache.misses).isEqualTo(1);
    }

    @Test
    void search_returnsViewsBackedByRepository() {
        var iphone = Product.create("iPhone 16", "Apple flagship", Money.of("30000.00", "TWD"));
        var pixel = Product.create("Pixel 9", "Google phone", Money.of("25000.00", "TWD"));
        repo.save(iphone);
        repo.save(pixel);
        search.index(iphone.id(), iphone.name(), iphone.description());
        search.index(pixel.id(), pixel.name(), pixel.description());

        var results = handler.search("phone", 10);

        assertThat(results).extracting("name").contains("iPhone 16", "Pixel 9");
    }
}
