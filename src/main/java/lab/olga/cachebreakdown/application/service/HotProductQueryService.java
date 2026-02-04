package lab.olga.cachebreakdown.application.service;

import lab.olga.cachebreakdown.domain.model.Product;
import lab.olga.cachebreakdown.infrastructure.cache.CacheKeys;
import lab.olga.cachebreakdown.infrastructure.cache.HotProductCacheFacade;
import lab.olga.cachebreakdown.infrastructure.cache.RedisMutex;
import lab.olga.cachebreakdown.infrastructure.cache.config.HotProductsCacheProperties;
import lab.olga.cachebreakdown.infrastructure.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class HotProductQueryService {

    private final HotProductCacheFacade cache;
    private final ProductRepository repo;
    private final RedisMutex mutex;
    private final HotProductsCacheProperties props;

    public HotProductQueryService(
            HotProductCacheFacade cache,
            ProductRepository repo,
            RedisMutex mutex,
            HotProductsCacheProperties props
    ) {
        this.cache = cache;
        this.repo = repo;
        this.mutex = mutex;
        this.props = props;
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

        // 3) tryLock 嘗試取得 mutex
        Duration lockTtl = Duration.ofMillis(props.getMutex().getLockTtlMs());
        String token = mutex.tryLock(CacheKeys.LOCK_HOT_PRODUCTS_TOP10, lockTtl);

        if (token == null) {
            // 3-1) 沒拿到鎖：backoff + jitter retry L2（single-flight）
            int retryTimes = props.getMutex().getRetryTimes();
            long base = props.getMutex().getRetryBaseSleepMs();
            long max = props.getMutex().getRetryMaxSleepMs();

            for (int i = 0; i < retryTimes; i++) {
                // Exponential backoff + jitter：逐次拉長等待時間並打散重試節奏，避免 thundering herd（大量執行緒同步醒來重試）造成 重試風暴 與 不必要的 Redis 壓力
                sleep(withBackoffAndJitter(base, max, i));
                var retry = cache.getHotTop10FromRedis();
                if (retry.isPresent()) {
                    cache.putHotTop10ToL1(retry.get());// L1 是 per-instance cache；從 L2 命中後回填本機 L1，可降低後續同節點對 Redis 的讀壓力
                    return new Result("REDIS", retry.get(), repo.getDbHitCount());
                }
            }

            // 3-2) 最後降級策略（明確）：直接回源 DB（少量回源可接受）
            //保底（fallback）策略：若在合理等待時間內仍未等到 L2 回填，則選擇少量回源 DB 以避免請求長時間卡死與重試風暴；可用性與 DB 保護間的取捨。
            List<Product> db = repo.findHotTop10();
            cache.putHotTop10ToRedis(db, props.getL2().getTtlSeconds(), props.getL2().getJitterSeconds());
            cache.putHotTop10ToL1(db);
            return new Result("DB", db, repo.getDbHitCount());
        }

        try {
            // 4) double-check L2
            var again = cache.getHotTop10FromRedis();
            if (again.isPresent()) {
                cache.putHotTop10ToL1(again.get());
                return new Result("REDIS", again.get(), repo.getDbHitCount());
            }

            // 5) 抓 DB資料、回填L2、L1
            List<Product> db = repo.findHotTop10();
            cache.putHotTop10ToRedis(db, props.getL2().getTtlSeconds(), props.getL2().getJitterSeconds());
            cache.putHotTop10ToL1(db);

            return new Result("DB", db, repo.getDbHitCount());
        } finally {
            mutex.unlock(CacheKeys.LOCK_HOT_PRODUCTS_TOP10, token);
        }
    }

    /**
     * 「越失敗越慢一點再試」，避免重試風暴」 <br>
     * 第 0 次：base <br>
     * 第 1 次：2*base  <br>
     * 第 2 次：4*base  <br>
     * …直到 max 上限  <br>
     * 再加一點 jitter 讓大家醒來時間錯開  <br>
     */
    private long withBackoffAndJitter(long baseMs, long maxMs, int attempt) {
        // base * 2^attempt capped + small jitter
        long backoff = Math.min(maxMs, baseMs * (1L << Math.min(attempt, 10)));
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, backoff / 4));
        return backoff + jitter;
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