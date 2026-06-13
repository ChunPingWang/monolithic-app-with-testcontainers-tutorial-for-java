package com.tutorial.ecommerce.product.application.query;

import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.product.domain.port.inbound.QueryProductUseCase;
import com.tutorial.ecommerce.product.domain.port.outbound.CachePort;
import com.tutorial.ecommerce.product.domain.port.outbound.ProductWriteRepository;
import com.tutorial.ecommerce.product.domain.port.outbound.SearchPort;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class SearchProductQueryHandler implements QueryProductUseCase {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private final ProductWriteRepository products;
    private final SearchPort search;
    private final CachePort cache;

    public SearchProductQueryHandler(ProductWriteRepository products, SearchPort search, CachePort cache) {
        this.products = products;
        this.search = search;
        this.cache = cache;
    }

    @Override
    public Optional<ProductView> findById(ProductId id) {
        var cacheKey = "product:" + id.value();
        var cached = cache.get(cacheKey, ProductView.class);
        if (cached.isPresent()) return cached;

        return products.findById(id)
            .map(this::toView)
            .map(view -> {
                cache.put(cacheKey, view, CACHE_TTL);
                return view;
            });
    }

    @Override
    public List<ProductView> search(String keyword, int limit) {
        var ids = search.search(keyword, limit);
        return ids.stream()
            .map(products::findById)
            .flatMap(Optional::stream)
            .map(this::toView)
            .toList();
    }

    private ProductView toView(Product p) {
        return new ProductView(p.id(), p.name(), p.description(), p.price(), p.imageObjectKey());
    }
}
