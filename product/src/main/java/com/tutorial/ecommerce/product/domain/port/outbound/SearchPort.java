package com.tutorial.ecommerce.product.domain.port.outbound;

import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

import java.util.List;

public interface SearchPort {

    /** 索引/更新單一商品 (id, name, description)。 */
    void index(ProductId id, String name, String description);

    void remove(ProductId id);

    /** 全文檢索;回傳符合的 productId(由上層再查 DB 取完整資料)。 */
    List<ProductId> search(String query, int limit);
}
