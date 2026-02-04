package lab.olga.cachebreakdown.adapter.web;

import lab.olga.cachebreakdown.adapter.web.dto.HotProductsResponse;
import lab.olga.cachebreakdown.application.service.HotProductQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductHotController {

    private final HotProductQueryService service;

    public ProductHotController(HotProductQueryService service) {
        this.service = service;
    }

    @GetMapping("/api/products/hot")
    public HotProductsResponse hotTop10() {
        var r = service.getHotTop10();
        return new HotProductsResponse(r.cacheLevel(), r.products(), r.dbHitCount());
    }
}