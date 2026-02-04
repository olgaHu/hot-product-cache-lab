# hot-product-cache-lab
高併發熱門商品查詢的快取與防擊穿實作（Redis + Caffeine）

專案模擬高併發熱點讀取場景下，
如何透過 Caffeine ＋ Redis 多級快取，
並搭配 基於互斥鎖 的快取擊穿防護機制，
以降低後端資料來源的壓力並提升整體系統穩定性。
