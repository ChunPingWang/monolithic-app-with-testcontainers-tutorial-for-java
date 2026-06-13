package com.tutorial.ecommerce.product.adapter.outbound.persistence;

import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.product.domain.port.outbound.ProductWriteRepository;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaProductRepository implements ProductWriteRepository {

    private final ProductJpaRepositorySpringData delegate;

    public JpaProductRepository(ProductJpaRepositorySpringData delegate) {
        this.delegate = delegate;
    }

    @Override
    public Product save(Product product) {
        return delegate.save(ProductJpaEntity.fromDomain(product)).toDomain();
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return delegate.findById(id.value()).map(ProductJpaEntity::toDomain);
    }
}
