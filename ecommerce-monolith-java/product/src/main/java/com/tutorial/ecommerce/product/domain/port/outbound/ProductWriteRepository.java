package com.tutorial.ecommerce.product.domain.port.outbound;

import com.tutorial.ecommerce.product.domain.model.Product;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

import java.util.Optional;

public interface ProductWriteRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId id);
}
