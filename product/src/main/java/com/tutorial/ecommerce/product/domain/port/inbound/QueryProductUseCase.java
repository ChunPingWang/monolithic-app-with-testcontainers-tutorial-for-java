package com.tutorial.ecommerce.product.domain.port.inbound;

import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

import java.util.List;
import java.util.Optional;

public interface QueryProductUseCase {

    Optional<ProductView> findById(ProductId id);

    List<ProductView> search(String keyword, int limit);

    record ProductView(ProductId id, String name, String description, Money price, String imageObjectKey) {}
}
