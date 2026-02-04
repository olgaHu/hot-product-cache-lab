package lab.olga.cachebreakdown.infrastructure.repo;

import lab.olga.cachebreakdown.domain.model.Product;

import java.util.List;

/**
 *  模擬 DB hit + sleep + counter
 */
public interface ProductRepository {
    List<Product> findHotTop10();
    long getDbHitCount(); // demo 用：觀察是否擊穿
}