package lab.olga.cachebreakdown.infrastructure.cache;

public final class CacheKeys {
    private CacheKeys() {}

    // Redis data key
    public static final String HOT_PRODUCTS_TOP10 = "hot:products:top10";

    // 分布式互斥鎖
    public static final String LOCK_HOT_PRODUCTS_TOP10 = "lock:hot:products:top10";

    // L1 key
    public static final String L1_HOT_PRODUCTS_TOP10 = HOT_PRODUCTS_TOP10;
}