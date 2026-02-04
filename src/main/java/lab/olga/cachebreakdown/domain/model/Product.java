package lab.olga.cachebreakdown.domain.model;

import java.math.BigDecimal;

public record Product(
        Long id,
        String name,
        BigDecimal price,
        Long soldCount
) {}