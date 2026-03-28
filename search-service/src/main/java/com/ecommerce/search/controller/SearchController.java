package com.ecommerce.search.controller;

import com.ecommerce.search.model.Product;
import com.ecommerce.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(@RequestParam String q) {
        List<Product> results = searchService.search(q);
        return ResponseEntity.ok(Map.of(
                "query", q,
                "total", results.size(),
                "results", results
        ));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<Map<String, Object>> autocomplete(@RequestParam String q) {
        List<String> suggestions = searchService.autocomplete(q);
        return ResponseEntity.ok(Map.of(
                "prefix", q,
                "suggestions", suggestions
        ));
    }

    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filter(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice
    ) {
        List<Product> results = searchService.filter(category, minPrice, maxPrice);
        return ResponseEntity.ok(Map.of(
                "filters", Map.of(
                        "category", category != null ? category : "all",
                        "minPrice", minPrice != null ? minPrice : "none",
                        "maxPrice", maxPrice != null ? maxPrice : "none"
                ),
                "total", results.size(),
                "results", results
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "search-service"
        ));
    }
}