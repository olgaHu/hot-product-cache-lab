package lab.olga.cachebreakdown.infrastructure.cache.config;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lab.olga.cachebreakdown.domain.model.Product;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<String, List<Product>> hotProductsL1Cache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS) // L1: 模擬 短TTL
                .maximumSize(1000)
                .build();
    }
}