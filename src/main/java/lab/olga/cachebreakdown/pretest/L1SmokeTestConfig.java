package lab.olga.cachebreakdown.pretest;

import lab.olga.cachebreakdown.infrastructure.cache.HotProductCacheFacade;
import lab.olga.cachebreakdown.infrastructure.repo.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class L1SmokeTestConfig {

    @Bean
    CommandLineRunner l1Smoke(ProductRepository repo, HotProductCacheFacade cache) {
        return args -> {
            System.out.println("L1 first read = " + cache.getHotTop10FromL1().isPresent()); // false

            var data = repo.findHotTop10();
            cache.putHotTop10ToL1(data);

            System.out.println("L1 second read = " + cache.getHotTop10FromL1().isPresent()); // true
            System.out.println("DB hits = " + repo.getDbHitCount()); // 1
        };
    }
}