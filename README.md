# 高併發熱門商品查詢的快取與防擊穿實作（Redis + Caffeine）

本SideProject實作在「高併發熱門商品（Top 10）查詢」場景下，  
如何透過 **多級快取 + Redis 分布式互斥鎖**，避免快取擊穿（Cache Breakdown）。

---

## 設計摘要

### 多級快取（Caffeine + Redis）
- 使用 **L1（Caffeine）本地快取** 吸收瞬間高併發請求
- 降低 Redis 存取頻率與鎖競爭
- L1 為 per-instance cache（每個 JVM / Pod 各自一份）

---

### 防止擊穿（Cache Breakdown）
- 在 Redis cache miss 時，使用 **分布式 mutex（SET NX PX）**
- 確保同一時間只有一個請求回源資料庫
- 其餘請求等待回填結果並重用快取資料

設計重點包含：
- Redis Mutex（SET NX PX）
- Double-check Redis（避免重複回源）
- Retry + Exponential Backoff + Jitter（避免重試風暴）
- Fallback 策略（避免請求卡死）

---

----
## 測試說明
### 1. 快取層級與過期時間（L1 / L2）
為方便觀察行為，TTL 預設為較短時間：

  * L1 Cache（Caffeine） TTL：10 秒
  * L2 Cache（Redis） TTL：30 秒 + 隨機 jitter（0～10 秒）

### 2. 查詢熱門商品 API
curl http://localhost:8080/api/products/hot

### 3. 回傳格式範例
```json
{
  "cacheLevel": "DB",
  "products": [...],
  "dbHitCount": 1
}
```
* 說明：
  * cacheLevel：本次請求命中的層級（L1 / REDIS / DB）
  * dbHitCount：模擬實際回源資料庫的次數，用於觀察是否發生擊穿

### 4. 測試驗證重點
1️⃣ 第一次請求
* L1 / L2 未命中
* 回源 DB，並回填 L2（Redis）與 L1（Caffeine）
* cacheLevel = DB

2️⃣ 短時間內再次請求
* 命中 L1 或 L2
* 不再回源 DB
* cacheLevel = L1 或 REDIS

3️⃣ Redis 過期瞬間的高併發
* 僅一個請求取得 mutex 並回源 DB
* 其餘請求等待並從 L2 取得結果（single-flight）
* dbHitCount 在單一 Redis TTL 週期內約增加 1 次

---

## 附註
* 本 demo 使用 [**短 TTL**] 避免死鎖
* 若實務場景中回源 DB 耗時較長，可加入 watchdog（鎖續租）機制，以降低重複回源機率