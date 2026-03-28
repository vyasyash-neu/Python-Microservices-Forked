package com.ecommerce.search.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.util.List;

@Document(indexName = "products")
public class Product {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Text)
    private String imageUrl;

    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    @CompletionField(maxInputLength = 100)
    private Completion suggest;

    public Product() {}

    public Product(String id, String name, String description, Double price,
                   String category, List<String> tags, String imageUrl,
                   Integer stockQuantity, Completion suggest) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.tags = tags;
        this.imageUrl = imageUrl;
        this.stockQuantity = stockQuantity;
        this.suggest = suggest;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Completion getSuggest() { return suggest; }
    public void setSuggest(Completion suggest) { this.suggest = suggest; }

    // Builder pattern
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, name, description, category, imageUrl;
        private Double price;
        private List<String> tags;
        private Integer stockQuantity;
        private Completion suggest;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder price(Double price) { this.price = price; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder stockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; return this; }
        public Builder suggest(Completion suggest) { this.suggest = suggest; return this; }

        public Product build() {
            return new Product(id, name, description, price, category, tags, imageUrl, stockQuantity, suggest);
        }
    }
}