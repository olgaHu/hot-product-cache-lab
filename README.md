# 高併發熱門商品查詢的快取與防擊穿實作（Redis + Caffeine）

## 多級快取 (Caffeine ＋ Redis):
  * 透過 本地 Caffeine 降低 Redis 壓力、降低鎖競爭
## 防止擊穿 :  
  * 以 互斥鎖 機制 防護 快取擊穿
    * 在 Redis cache miss 時，使用分布式 mutex（SET NX PX）確保 single-flight 回源，避免高併發造成快取擊穿。
## 防止雪崩
  * Jitter 防止雪崩

## 防止穿透
if db_not_found:
redis.set(key, "NULL", ttl=60)

---
## 測試
### 快取層級 與 過期時間（L1 / L2）
 實作 **L1（Caffeine）+ L2（Redis）多級快取**，

### 快取設定
方便模擬，TTL設定時間
- **L1 Cache（Caffeine）** TTL：10 秒
- **L2 Cache（Redis）** TTL：30 秒 + 隨機 jitter（0～10 秒）
   
---

### API 測試
  * API
    * curl http://localhost:8080/api/products/hot
  * 回傳格式 
    - {
      "cacheLevel": "DB",  
      "products": [略] ,
      "dbHitCount": 1
      }
  * 多次呼叫此 API ，預期 cacheLevel 的變化 與  dbHitCount
    * cacheLevel 的值 ( L1 / REDIS / DB)
    * dbHitCount : 實際擊中DB 的次數

flowchart TD
A[Client GET /api/products/hot] --> B{L1 Caffeine hit?}
B -->|Yes| R1[Return: cacheLevel=L1]
B -->|No| C{L2 Redis hit?}
C -->|Yes| D[Put into L1] --> R2[Return: cacheLevel=REDIS]
C -->|No| E{Try acquire Redis mutex?}
E -->|No| W[Wait short + retry L2] --> C
E -->|Yes| F[Double-check L2 Redis hit?]
F -->|Yes| D2[Put into L1] --> R3[Return: cacheLevel=REDIS]
F -->|No| G[Query Fake DB] --> H[Set Redis (TTL+jitter)]
H --> I[Put into L1] --> R4[Return: cacheLevel=DB]
R4 --> J[Release mutex]
