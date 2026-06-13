package com.tutorial.ecommerce.product.application.fake;

import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.product.domain.port.outbound.ProductWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProductRepository implements ProductWriteRepository {

    private final Map<ProductId, Product> store = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        store.put(product.id(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return Optional.ofNullable(store.get(id));
    }
}
