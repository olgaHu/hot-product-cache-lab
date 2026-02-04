package lab.olga.cachebreakdown.application.service;

/**
 * 先查 L1（Caffeine） <br>
 *   # miss → 查 L2（Redis）<br>
 *   # miss → 嘗試拿 Redis mutex<br>
 *     - 拿不到：sleep 50~100ms，重試 Redis（最多 N 次）<br>
 *     - 拿到：double-check Redis<br>
 *       -- 如果還 miss：打 Fake DB → 回填 Redis → 回填 L1<br>
 * 回傳結果 + cacheLevel<br>
 */

import lab.olga.cachebreakdown.domain.model.Product;
import lab.olga.cachebreakdown.infrastructure.cache.CacheKeys;
import lab.olga.cachebreakdown.infrastructure.cache.HotProductCacheFacade;
import lab.olga.cachebreakdown.infrastructure.cache.RedisMutex;
import lab.olga.cachebreakdown.infrastructure.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class HotProductQueryService {

    private final HotProductCacheFacade cache;
    private final ProductRepository repo;
    private final RedisMutex mutex;

    private static final int REDIS_TTL_SECONDS = 30;
    private static final int REDIS_TTL_JITTER_SECONDS = 10;

    private static final Duration LOCK_TTL = Duration.ofSeconds(3);
    private static final int RETRY_TIMES = 5;
    private static final long RETRY_SLEEP_MS = 80;

    public HotProductQueryService(
            HotProductCacheFacade cache,
            ProductRepository repo,
            RedisMutex mutex
    ) {
        this.cache = cache;
        this.repo = repo;
        this.mutex = mutex;
    }

    public Result getHotTop10() {

        // 1) L1
        var l1 = cache.getHotTop10FromL1();
        if (l1.isPresent()) {
            return new Result("L1", l1.get(), repo.getDbHitCount());
        }

        // 2) L2
        var l2 = cache.getHotTop10FromRedis();
        if (l2.isPresent()) {
            cache.putHotTop10ToL1(l2.get());
            return new Result("REDIS", l2.get(), repo.getDbHitCount());
        }

        // 3) 嘗試取得 mutex
        String token = mutex.tryLock(CacheKeys.LOCK_HOT_PRODUCTS_TOP10, LOCK_TTL);

        if (token == null) {
            // 3-1) 拿不到鎖：等待 + 重試 Redis（single-flight）
            for (int i = 0; i < RETRY_TIMES; i++) {
                sleep(RETRY_SLEEP_MS);
                var retry = cache.getHotTop10FromRedis();
                if (retry.isPresent()) {
                    cache.putHotTop10ToL1(retry.get());
                    return new Result("REDIS", retry.get(), repo.getDbHitCount());
                }
            }
            // 最後降級（理論上很少發生）
        }

        try {
            // 4) double-check Redis（避免重複回源）
            var again = cache.getHotTop10FromRedis();
            if (again.isPresent()) {
                cache.putHotTop10ToL1(again.get());
                return new Result("REDIS", again.get(), repo.getDbHitCount());
            }

            // 5) 回源 DB（只有拿到鎖的人會走到這）
            List<Product> db = repo.findHotTop10();
            cache.putHotTop10ToRedis(db, REDIS_TTL_SECONDS, REDIS_TTL_JITTER_SECONDS);//回填L2
            cache.putHotTop10ToL1(db);//回填L1

            return new Result("DB", db, repo.getDbHitCount());

        } finally {
            if (token != null) {
                mutex.unlock(CacheKeys.LOCK_HOT_PRODUCTS_TOP10, token);
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record Result(String cacheLevel, List<Product> products, long dbHitCount) {}
}