package lab.olga.cachebreakdown.pretest;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisPingController {

    private final StringRedisTemplate redis;

    public RedisPingController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @PostMapping("/set")
    public Map<String, Object> set(
            @RequestParam("key") String key,
            @RequestParam("value") String value
    ) {
        redis.opsForValue().set(key, value, Duration.ofSeconds(30));

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("ok", true);
        res.put("key", key);
        res.put("value", value);
        return res;
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam("key") String key) {
        String value = redis.opsForValue().get(key);

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("key", key);
        res.put("value", value);
        return res;
    }


}
