package com.ecommerce.search.scheduler;

import com.ecommerce.search.model.Product;
import com.ecommerce.search.service.SearchService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiffReconcileJobTest {

    @Mock
    private SearchService searchService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder builder;

    private DiffReconcileJob job;

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(restTemplate);
        job = new DiffReconcileJob(searchService, builder, "http://product:8001", true);
    }

    // ─── Disabled flag ───────────────────────────────────────────────────────

    @Test
    void diffAndReconcile_doesNothing_whenDisabled() {
        DiffReconcileJob disabled = new DiffReconcileJob(searchService, builder, "http://product:8001", false);

        disabled.diffAndReconcile();

        verifyNoInteractions(restTemplate);
        verify(searchService, never()).indexProduct(any());
        verify(searchService, never()).deleteProduct(any());
    }

    // ─── Upsert path ─────────────────────────────────────────────────────────

    @Test
    void diffAndReconcile_upsertsAllMongoProducts() {
        mockProductServiceReturns(List.of(
                productMap("1", "iPhone"),
                productMap("2", "Samsung")
        ));
        when(searchService.getAll()).thenReturn(List.of());

        job.diffAndReconcile();

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(searchService, times(2)).indexProduct(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("iPhone", "Samsung");
    }

    // ─── Delete path (the killer test for orphans) ───────────────────────────

    @Test
    void diffAndReconcile_deletesOrphansInEs() {
        mockProductServiceReturns(List.of(
                productMap("1", "iPhone")
        ));

        Product orphan = new Product();
        orphan.setId("999");
        orphan.setName("Phantom");
        when(searchService.getAll()).thenReturn(List.of(orphan));

        job.diffAndReconcile();

        verify(searchService).deleteProduct("999");
    }

    @Test
    void diffAndReconcile_doesNotDelete_whenIdsMatch() {
        mockProductServiceReturns(List.of(
                productMap("1", "iPhone")
        ));

        Product inSync = new Product();
        inSync.setId("1");
        inSync.setName("iPhone");
        when(searchService.getAll()).thenReturn(List.of(inSync));

        job.diffAndReconcile();

        verify(searchService).indexProduct(any());        // upsert still happens
        verify(searchService, never()).deleteProduct(any()); // but no delete
    }

    // ─── Mixed scenario ──────────────────────────────────────────────────────

    @Test
    void diffAndReconcile_mixedUpsertAndDelete() {
        mockProductServiceReturns(List.of(
                productMap("1", "iPhone"),
                productMap("2", "Samsung")
        ));

        Product staleEsProduct = new Product();
        staleEsProduct.setId("999");
        when(searchService.getAll()).thenReturn(List.of(staleEsProduct));

        job.diffAndReconcile();

        verify(searchService, times(2)).indexProduct(any());
        verify(searchService).deleteProduct("999");
    }

    // ─── Empty cases ─────────────────────────────────────────────────────────

    @Test
    void diffAndReconcile_handlesEmptyMongo() {
        mockProductServiceReturns(List.of());
        when(searchService.getAll()).thenReturn(List.of());

        job.diffAndReconcile();

        verify(searchService, never()).indexProduct(any());
        verify(searchService, never()).deleteProduct(any());
    }

    @Test
    void diffAndReconcile_deletesAll_whenMongoIsEmpty() {
        mockProductServiceReturns(List.of());

        Product orphan1 = new Product(); orphan1.setId("1");
        Product orphan2 = new Product(); orphan2.setId("2");
        when(searchService.getAll()).thenReturn(List.of(orphan1, orphan2));

        job.diffAndReconcile();

        verify(searchService).deleteProduct("1");
        verify(searchService).deleteProduct("2");
        verify(searchService, never()).indexProduct(any());
    }

    // ─── Error handling ──────────────────────────────────────────────────────

    @Test
    void diffAndReconcile_swallowsProductServiceErrors() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("Product Service down"));

        // Should not throw — error is logged and swallowed so next cycle retries
        job.diffAndReconcile();

        verify(searchService, never()).indexProduct(any());
        verify(searchService, never()).deleteProduct(any());
    }

    @Test
    void diffAndReconcile_handlesNullResponseFromProductService() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);
        when(searchService.getAll()).thenReturn(List.of());

        job.diffAndReconcile();

        verify(searchService, never()).indexProduct(any());
    }

    // ─── Pagination ──────────────────────────────────────────────────────────

    @Test
    void diffAndReconcile_paginatesAcrossMultiplePages() {
        // 100 products on page 1, 1 on page 2 → 2 page calls
        List<Map<String, Object>> page1 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            page1.add(productMap("p" + i, "Product" + i));
        }
        List<Map<String, Object>> page2 = List.of(productMap("p100", "Product100"));

        when(restTemplate.getForObject(contains("page=1"), eq(Map.class)))
                .thenReturn(Map.of("products", page1));
        when(restTemplate.getForObject(contains("page=2"), eq(Map.class)))
                .thenReturn(Map.of("products", page2));

        when(searchService.getAll()).thenReturn(List.of());

        job.diffAndReconcile();

        verify(searchService, times(101)).indexProduct(any());
        verify(restTemplate).getForObject(contains("page=1"), eq(Map.class));
        verify(restTemplate).getForObject(contains("page=2"), eq(Map.class));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void mockProductServiceReturns(List<Map<String, Object>> products) {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("products", products));
    }

    private Map<String, Object> productMap(String id, String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("description", "desc");
        m.put("price", 99.99);
        m.put("category", "cat");
        m.put("tags", List.of("tag1"));
        m.put("stock_quantity", 10);
        return m;
    }
}