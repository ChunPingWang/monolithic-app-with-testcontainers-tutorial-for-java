package com.tutorial.ecommerce.product.application.fake;

import com.tutorial.ecommerce.product.domain.port.outbound.SearchPort;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySearchAdapter implements SearchPort {

    private record Doc(String name, String description) {}
    private final Map<ProductId, Doc> index = new ConcurrentHashMap<>();

    @Override
    public void index(ProductId id, String name, String description) {
        index.put(id, new Doc(name == null ? "" : name, description == null ? "" : description));
    }

    @Override
    public void remove(ProductId id) {
        index.remove(id);
    }

    @Override
    public List<ProductId> search(String query, int limit) {
        var lower = query.toLowerCase();
        var hits = new ArrayList<ProductId>();
        for (var entry : index.entrySet()) {
            var doc = entry.getValue();
            if (doc.name().toLowerCase().contains(lower) || doc.description().toLowerCase().contains(lower)) {
                hits.add(entry.getKey());
                if (hits.size() >= limit) break;
            }
        }
        return hits;
    }
}
