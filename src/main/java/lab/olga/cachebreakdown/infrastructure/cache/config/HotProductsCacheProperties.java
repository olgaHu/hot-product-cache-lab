package lab.olga.cachebreakdown.infrastructure.cache.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.hot-products")
public class HotProductsCacheProperties {

    private final L2 l2 = new L2();
    private final Mutex mutex = new Mutex();

    public L2 getL2() { return l2; }
    public Mutex getMutex() { return mutex; }

    public static class L2 {
        private int ttlSeconds = 30;
        private int jitterSeconds = 10;

        public int getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
        public int getJitterSeconds() { return jitterSeconds; }
        public void setJitterSeconds(int jitterSeconds) { this.jitterSeconds = jitterSeconds; }
    }

    public static class Mutex {
        private long lockTtlMs = 3000;
        private int retryTimes = 6;
        private long retryBaseSleepMs = 40;
        private long retryMaxSleepMs = 200;

        public long getLockTtlMs() { return lockTtlMs; }
        public void setLockTtlMs(long lockTtlMs) { this.lockTtlMs = lockTtlMs; }
        public int getRetryTimes() { return retryTimes; }
        public void setRetryTimes(int retryTimes) { this.retryTimes = retryTimes; }
        public long getRetryBaseSleepMs() { return retryBaseSleepMs; }
        public void setRetryBaseSleepMs(long retryBaseSleepMs) { this.retryBaseSleepMs = retryBaseSleepMs; }
        public long getRetryMaxSleepMs() { return retryMaxSleepMs; }
        public void setRetryMaxSleepMs(long retryMaxSleepMs) { this.retryMaxSleepMs = retryMaxSleepMs; }
    }
}