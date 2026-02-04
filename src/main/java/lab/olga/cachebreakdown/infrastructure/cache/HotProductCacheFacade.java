package lab.olga.cachebreakdown.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lab.olga.cachebreakdown.domain.model.Product;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 *  L1/L2 讀寫封裝（集中管理）<br>
 *  封裝 L1/L2 操作 <br>
 */

@Component
public class HotProductCacheFacade {

    private final Cache<String, List<Product>> l1Cache;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final TypeReference<List<Product>> LIST_OF_PRODUCT =
            new TypeReference<>() {};

    public HotProductCacheFacade(
            Cache<String, List<Product>> hotProductsL1Cache,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper redisObjectMapper
    ) {
        this.l1Cache = hotProductsL1Cache;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisObjectMapper = redisObjectMapper;
    }

    // -------- L1 (Caffeine) ---------------------------------------
    public Optional<List<Product>> getHotTop10FromL1() {
        return Optional.ofNullable(l1Cache.getIfPresent(CacheKeys.L1_HOT_PRODUCTS_TOP10));
    }
    public void putHotTop10ToL1(List<Product> products) {
        if (products == null) return;
        l1Cache.put(CacheKeys.L1_HOT_PRODUCTS_TOP10, products);
    }
    public void evictHotTop10L1() {
        l1Cache.invalidate(CacheKeys.L1_HOT_PRODUCTS_TOP10);
    }

    // -------- L2 (Redis) ----------------------------------------
    public Optional<List<Product>> getHotTop10FromRedis() {
        String json = stringRedisTemplate.opsForValue().get(CacheKeys.HOT_PRODUCTS_TOP10);
        if (json == null || json.isBlank()) return Optional.empty();

        try {
            List<Product> list = redisObjectMapper.readValue(json, LIST_OF_PRODUCT);
            return Optional.of(list);
        } catch (Exception e) {
            stringRedisTemplate.delete(CacheKeys.HOT_PRODUCTS_TOP10);
            return Optional.empty();
        }
    }
    /**
     * ttlSeconds: base TTL（例如 30s）
     * jitterSeconds: 0~jitter 的隨機抖動，避免同時過期（雪崩）
     */
    public void putHotTop10ToRedis(List<Product> products, int ttlSeconds, int jitterSeconds) {
        if (products == null) return;

        int ttlWithJitter = ttlSeconds + ThreadLocalRandom.current().nextInt(0, jitterSeconds + 1);

        try {
            String json = redisObjectMapper.writeValueAsString(products);
            stringRedisTemplate.opsForValue()
                    .set(CacheKeys.HOT_PRODUCTS_TOP10, json, Duration.ofSeconds(ttlWithJitter));
        } catch (Exception e) {
            //log
        }
    }
}