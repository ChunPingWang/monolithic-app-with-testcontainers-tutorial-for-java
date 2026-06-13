package com.tutorial.ecommerce.product.adapter.outbound.search;

import com.tutorial.ecommerce.product.domain.port.outbound.SearchPort;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Component
@ConditionalOnProperty(name = "ecommerce.adapter.search", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchSearchAdapter implements SearchPort {

    private final ElasticsearchOperations operations;

    public ElasticsearchSearchAdapter(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void index(ProductId id, String name, String description) {
        operations.save(new ProductSearchDoc(id.value().toString(), name, description));
    }

    @Override
    public void remove(ProductId id) {
        operations.delete(id.value().toString(), ProductSearchDoc.class);
    }

    @Override
    public List<ProductId> search(String query, int limit) {
        NativeQuery nq = NativeQuery.builder()
            .withQuery(q -> q.multiMatch(m -> m.fields("name", "description").query(query)))
            .withPageable(org.springframework.data.domain.PageRequest.of(0, limit))
            .build();
        SearchHits<ProductSearchDoc> hits = operations.search(nq, ProductSearchDoc.class);
        return StreamSupport.stream(hits.spliterator(), false)
            .map(h -> new ProductId(UUID.fromString(h.getContent().getId())))
            .toList();
    }
}
