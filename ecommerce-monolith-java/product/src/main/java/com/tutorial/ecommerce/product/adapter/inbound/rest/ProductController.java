package com.tutorial.ecommerce.product.adapter.inbound.rest;

import com.tutorial.ecommerce.product.domain.port.inbound.QueryProductUseCase;
import com.tutorial.ecommerce.product.domain.port.inbound.QueryProductUseCase.ProductView;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final QueryProductUseCase query;

    public ProductController(QueryProductUseCase query) {
        this.query = query;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductView> findOne(@PathVariable UUID id) {
        return query.findById(new ProductId(id))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<ProductView> search(
        @RequestParam(name = "q") String keyword,
        @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return query.search(keyword, limit);
    }
}
