package lab.olga.cachebreakdown.adapter.web.dto;

import lab.olga.cachebreakdown.domain.model.Product;

import java.util.List;

public record HotProductsResponse(
        String cacheLevel,        // "L1" | "REDIS" | "DB"
        List<Product> products,
        long dbHitCount           // demo 用：觀察 DB hit 次數
) {}
