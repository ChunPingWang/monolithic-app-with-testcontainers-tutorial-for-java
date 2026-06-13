package com.tutorial.ecommerce.product.domain.model;

import com.tutorial.ecommerce.product.domain.DomainException;
import com.tutorial.ecommerce.sharedkernel.domain.Money;
import com.tutorial.ecommerce.sharedkernel.domain.ProductId;

public class Product {

    private final ProductId id;
    private String name;
    private String description;
    private Money price;
    private String imageObjectKey;
    private long version;

    private Product(ProductId id, String name, String description, Money price, String imageObjectKey, long version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageObjectKey = imageObjectKey;
        this.version = version;
    }

    public static Product create(String name, String description, Money price) {
        if (name == null || name.isBlank()) throw new DomainException("name required");
        if (price.isNegative()) throw new DomainException("price must be non-negative");
        return new Product(ProductId.newId(), name, description, price, null, 0L);
    }

    public static Product rehydrate(ProductId id, String name, String description, Money price, String imageObjectKey, long version) {
        return new Product(id, name, description, price, imageObjectKey, version);
    }

    public void changePrice(Money newPrice) {
        if (newPrice.isNegative()) throw new DomainException("price must be non-negative");
        this.price = newPrice;
    }

    public void attachImage(String objectKey) {
        this.imageObjectKey = objectKey;
    }

    public ProductId id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Money price() { return price; }
    public String imageObjectKey() { return imageObjectKey; }
    public long version() { return version; }
}
