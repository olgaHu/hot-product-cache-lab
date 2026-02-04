package lab.olga.cachebreakdown.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 *  Redis 分布式鎖
 */
@Component
public class RedisMutex {

    private final StringRedisTemplate redis;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );

    public RedisMutex(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 嘗試取得鎖，成功回傳 token，失敗回傳 null */
    public String tryLock(String lockKey, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey, token, ttl);//SET NX PX
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /** 只解自己的鎖 */
    public void unlock(String lockKey, String token) {
        redis.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
    }
}