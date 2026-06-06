package com.ecommerce.search.kafka;

import com.ecommerce.search.model.Product;
import com.ecommerce.search.service.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    public ProductEventConsumer(SearchService searchService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "product-updated", groupId = "search-service-group")
    public void handleProductUpdated(String message) {
        try {
            log.info("📡 Received product-updated event");
            JsonNode root = objectMapper.readTree(message);

            String eventType = root.has("event_type") ? root.get("event_type").asText() : "PRODUCT_UPDATED";
            String productId = root.has("product_id") ? root.get("product_id").asText() : null;

            // Handle delete: payload has no product body
            if ("PRODUCT_DELETED".equals(eventType) || "DELETED".equals(eventType)) {
                if (productId != null) {
                    searchService.deleteProduct(productId);
                    log.info("🗑️ Product deleted from index: {}", productId);
                }
                return;
            }

            // For CREATE/UPDATE: unwrap nested "product" object if present, else use root (back-compat)
            JsonNode node = root.has("product") && !root.get("product").isNull()
                    ? root.get("product")
                    : root;

            Product product = Product.builder()
                    .id(getTextOrNull(node, "id", "_id"))
                    .name(getTextOrNull(node, "name"))
                    .description(getTextOrNull(node, "description"))
                    .price(node.has("price") && !node.get("price").isNull() ? node.get("price").asDouble() : null)
                    .category(getTextOrNull(node, "category"))
                    .tags(getStringList(node, "tags"))
                    .imageUrl(getTextOrNull(node, "image_url"))
                    .stockQuantity(node.has("stock_quantity") && !node.get("stock_quantity").isNull() ? node.get("stock_quantity").asInt() : null)
                    .build();

            // If id wasn't in the nested product (it usually is), fall back to root-level product_id
            if (product.getId() == null && productId != null) {
                product.setId(productId);
            }

            searchService.indexProduct(product);

        } catch (Exception e) {
            log.error("❌ Failed to process product-updated event: {}", e.getMessage(), e);
        }
    }
    
    private String getTextOrNull(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText();
            }
        }
        return null;
    }

    private List<String> getStringList(JsonNode node, String fieldName) {
        List<String> list = new ArrayList<>();
        if (node.has(fieldName) && node.get(fieldName).isArray()) {
            node.get(fieldName).forEach(item -> list.add(item.asText()));
        }
        return list;
    }
}