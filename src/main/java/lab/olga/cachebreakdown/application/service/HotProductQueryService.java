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
import lab.olga.cachebreakdown.infrastructure.cache.HotProductCacheFacade;
import lab.olga.cachebreakdown.infrastructure.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotProductQueryService {

    private final HotProductCacheFacade cache;
    private final ProductRepository repo;

    private static final int REDIS_TTL_SECONDS = 30;
    private static final int REDIS_TTL_JITTER_SECONDS = 10;

    public HotProductQueryService(HotProductCacheFacade cache, ProductRepository repo) {
        this.cache = cache;
        this.repo = repo;
    }

    public Result getHotTop10() {

        // 1. L1
        var l1 = cache.getHotTop10FromL1();
        if (l1.isPresent()) {
            return new Result("L1", l1.get(), repo.getDbHitCount());
        }

        // 2. L2
        var l2 = cache.getHotTop10FromRedis();
        if (l2.isPresent()) {
            cache.putHotTop10ToL1(l2.get());
            return new Result("REDIS", l2.get(), repo.getDbHitCount());
        }

        // 3. DB（Fake）
        List<Product> db = repo.findHotTop10();

        // 4. 回填 L2 + L1
        cache.putHotTop10ToRedis(db, REDIS_TTL_SECONDS, REDIS_TTL_JITTER_SECONDS);
        cache.putHotTop10ToL1(db);

        return new Result("DB", db, repo.getDbHitCount());
    }

    public record Result(String cacheLevel, List<Product> products, long dbHitCount) {}
}