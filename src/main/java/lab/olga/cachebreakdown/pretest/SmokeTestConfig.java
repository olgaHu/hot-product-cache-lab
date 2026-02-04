package lab.olga.cachebreakdown.pretest;

import lab.olga.cachebreakdown.infrastructure.repo.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class SmokeTestConfig {

    @Bean
    CommandLineRunner smoke(ProductRepository repo) {
        return args -> {
            repo.findHotTop10();
            System.out.println("DB hits = " + repo.getDbHitCount());
        };
    }
}