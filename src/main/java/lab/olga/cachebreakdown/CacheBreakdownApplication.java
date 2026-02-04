package lab.olga.cachebreakdown;

import lab.olga.cachebreakdown.infrastructure.cache.config.HotProductsCacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(HotProductsCacheProperties.class)
@SpringBootApplication
public class CacheBreakdownApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheBreakdownApplication.class, args);
    }
}
