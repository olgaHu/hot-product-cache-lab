package lab.olga.cachebreakdown.infrastructure.repo;

import lab.olga.cachebreakdown.domain.model.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


/**
 *  模擬 DB hit + sleep + counter
 */
@Repository
public class FakeProductRepository implements ProductRepository {

    private final AtomicLong dbHits = new AtomicLong(0);//每次回源 DB hit 都會 +1

    @Override
    public List<Product> findHotTop10() {
        dbHits.incrementAndGet();

        // 模擬 DB 慢查
        sleepRandom(80, 150);

        // 回傳固定 top10
        List<Product> list = new ArrayList<>(10);
        for (int i = 1; i <= 10; i++) {
            list.add(new Product(
                    (long) i,
                    "Hot Product " + i,
                    new BigDecimal("100").add(new BigDecimal(i * 10)),
                    10_000L - i
            ));
        }
        return list;
    }

    @Override
    public long getDbHitCount() {
        return dbHits.get();
    }

    private void sleepRandom(int minMs, int maxMs) {
        int delay = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

