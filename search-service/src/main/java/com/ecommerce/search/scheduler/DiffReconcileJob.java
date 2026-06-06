package com.ecommerce.search.scheduler;

import com.ecommerce.search.model.Product;
import com.ecommerce.search.service.SearchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class DiffReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(DiffReconcileJob.class);

    private final SearchService searchService;
    private final RestTemplate restTemplate;
    private final String productServiceUrl;
    private final boolean enabled;

    public DiffReconcileJob(
            SearchService searchService,
            RestTemplateBuilder builder,
            @Value("${product.service.url:http://localhost:8001}") String productServiceUrl,
            @Value("${reconciliation.enabled:true}") boolean enabled
    ) {
        this.searchService = searchService;
        this.restTemplate = builder.build();
        this.productServiceUrl = productServiceUrl;
        this.enabled = enabled;
    }

    /**
     * Diff-and-reconcile job. Runs every 30 minutes.
     *
     * MongoDB is the source of truth; Elasticsearch is a derived index.
     * This job reads from both stores to compute the diff, then writes
     * one-way — only to Elasticsearch. MongoDB is never modified here.
     *
     *   - Upserts products present in MongoDB (Mongo wins on every field)
     *   - Deletes ES docs whose IDs are no longer in MongoDB
     *
     * Safety net for Kafka outages, ES corruption, and schema drift.
     * The Kafka consumer handles the low-latency happy path; this job
     * guarantees eventual convergence.
     */
    @Scheduled(fixedDelayString = "${reconciliation.interval.ms:1800000}",
               initialDelayString = "${reconciliation.initial.delay.ms:60000}")
    public void diffAndReconcile() {
        if (!enabled) {
            log.debug("Reconciliation disabled, skipping");
            return;
        }

        log.info("🔄 Starting reconciliation: Product Service → Elasticsearch");
        long start = System.currentTimeMillis();

        try {
            List<Product> mongoProducts = fetchAllFromProductService();
            Map<String, Product> mongoById = mongoProducts.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

            Set<String> esIds = StreamSupport.stream(searchService.getAll().spliterator(), false)
                    .map(Product::getId)
                    .collect(Collectors.toSet());

            int upserts = 0;
            for (Product p : mongoProducts) {
                searchService.indexProduct(p);
                upserts++;
            }

            Set<String> toDelete = new HashSet<>(esIds);
            toDelete.removeAll(mongoById.keySet());
            for (String id : toDelete) {
                searchService.deleteProduct(id);
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("✅ Reconciliation done in {}ms — {} upserts, {} deletes",
                    elapsed, upserts, toDelete.size());
        } catch (Exception e) {
            log.error("❌ Reconciliation failed: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Product> fetchAllFromProductService() {
        List<Product> all = new ArrayList<>();
        int page = 1;
        int pageSize = 100;

        while (true) {
            String url = String.format("%s/api/products?page=%d&page_size=%d",
                    productServiceUrl, page, pageSize);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("products")) {
                log.warn("Unexpected response shape from Product Service on page {}", page);
                break;
            }

            List<Map<String, Object>> raw = (List<Map<String, Object>>) response.get("products");
            if (raw.isEmpty()) break;

            raw.stream().map(this::toProduct).forEach(all::add);

            if (raw.size() < pageSize) break;
            page++;
        }

        log.info("Fetched {} products from Product Service across {} page(s)", all.size(), page);
        return all;
    }

    @SuppressWarnings("unchecked")
    private Product toProduct(Map<String, Object> map) {
        Product p = new Product();
        p.setId((String) map.get("id"));
        p.setName((String) map.get("name"));
        p.setDescription((String) map.get("description"));
        Object price = map.get("price");
        if (price instanceof Number) {
            p.setPrice(((Number) price).doubleValue());
        }
        p.setCategory((String) map.get("category"));
        p.setTags((List<String>) map.getOrDefault("tags", List.of()));
        Object stock = map.get("stock_quantity");
        if (stock instanceof Number) {
            p.setStockQuantity(((Number) stock).intValue());
        }
        p.setImageUrl((String) map.get("image_url"));
        return p;
    }
}