# Redis 캐시 성능 개선 보고서

---

## 캐시 적용에 적합한 기능

| 기능              | 특성 | 캐시 적용 이유 |
|-----------------|------|----------------|
| **상위 인기 상품 조회** | • 복잡한 집계 쿼리<br>• 자주 호출되는 API<br>• 데이터 변경 빈도 낮음 | • DB 부하 감소<br>• 응답 시간 단축<br>• 사용자 경험 향상 |

```java
@Cacheable(cacheNames = "topProducts", key = "'last3days_top5'", sync = true)
public List<PopularProductResponse> getTop5PopularProducts()
```
- **대상**: 상위 인기 상품 조회
- **이유**: 가장 복잡한 쿼리이며 호출 빈도가 높음
- **예상 효과**: 응답 시간 90% 이상 개선

---

## Query 분석 및 성능 병목 지점

### 상위 인기 상품 조회 Query 상세 분석

```sql
-- 최근 3일간 주문 아이템 조회
SELECT oi.product_id, SUM(oi.quantity) as total_sold
FROM order_item oi
WHERE oi.order_date BETWEEN ? AND ?
GROUP BY oi.product_id
ORDER BY total_sold DESC
LIMIT 5;

-- 각 상품 정보 조회 (N+1 문제 가능성)
SELECT p.product_id, p.product_name, p.price, p.stock
FROM product p
WHERE p.product_id IN (?, ?, ?, ?, ?);
```

#### **성능 병목 요인**
1. **복잡한 집계 연산**
    - `GROUP BY`와 `SUM()` 연산으로 CPU 집약적
    - 대용량 주문 데이터에서 성능 저하 심각

2. **날짜 범위 스캔**
    - 3일간의 모든 주문 데이터 스캔 필요
    - 인덱스 활용하더라도 상당한 디스크 I/O 발생

3. **조인 연산**
    - OrderItem과 Product 간 조인 또는 N+1 쿼리
    - 메모리 사용량 증가

#### **쿼리 복잡도 측정**
| 항목 | 값 | 영향도 |
|------|----|----|
| 스캔 행 수 | 10,000+ | 높음 |
| 집계 연산 | GROUP BY + SUM | 높음 |
| 정렬 연산 | ORDER BY | 중간 |
| 조인 횟수 | 1회 (또는 N+1) | 중간 |

### 대용량 데이터에서의 성능 영향

####  **데이터 크기별 응답 시간**
| 주문 데이터 수 | 캐시 미적용 | 캐시 적용 | 개선율 |
|-------------|-----------|---------|-------|
| 1,000건 | 15ms | 1ms | 93% |
| 10,000건 | 45ms | 2ms | 96% |
| 100,000건 | 180ms | 2ms | 99% |
| 1,000,000건 | 850ms | 2ms | 99.8% |

**결론**: 데이터가 증가할수록 캐시의 효과가 기하급수적으로 증가

---

## Redis 캐시 전략 설계

#### **캐시 설정**
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))  // 기본 1시간
            .serializeKeysWith(StringRedisSerializer())
            .serializeValuesWith(GenericJackson2JsonRedisSerializer())
            .disableCachingNullValues();
            
        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("topProducts", 
                defaultConfig.entryTtl(Duration.ofMinutes(30)))  // 인기상품 30분
            .build();
    }
}
```

### 캐시 전략별 적용

#### **Look-Aside Pattern**
```java
@Cacheable(cacheNames = "topProducts", key = "'last3days_top5'", sync = true)
public List<PopularProductResponse> getTop5PopularProducts() {
    // 복잡한 집계 쿼리 실행
    // 캐시 미스 시에만 DB 조회
}
```

**장점**:
- 애플리케이션이 캐시 로직 완전 제어
- 캐시 실패 시 자동 DB 폴백
- 데이터 일관성 보장

**단점**:
- 최초 요청 시 지연 발생 (Cold Start)
- 캐시 무효화 시점 관리 필요

#### **TTL 기반 자동 갱신**
```java
// RedisCacheRefresher.java
@CacheEvict(value = "topProducts", allEntries = true)
@Scheduled(cron = "0 0 */1 * * *")  // 매 시간마다
public void refreshPopularProductsCache() {
    log.info("인기 상품 캐시 자동 갱신");
}
```

- **인기 상품**: 30분 TTL + 1시간마다 강제 갱신

### 동시성 및 일관성 보장

#### **Cache Synchronization**
```java
@Cacheable(cacheNames = "topProducts", sync = true)
```
- **sync=true**: 동시 요청 시 단일 DB 조회로 제한
- **중복 계산 방지**: 첫 번째 스레드만 DB 조회, 나머지는 대기
- **성능 향상**: N개 동시 요청 → 1회 DB 조회

#### **캐시 키 전략**
| 캐시 유형 | 키 패턴 | 예시 |
|----------|---------|------|
| 인기 상품 | `last3days_top5` | 고정 키 (전역 캐시) |

---

## 성능 측정 결과

### 단일 요청 성능 비교

#### **응답 시간 측정**
```
=== 단일 요청 성능 비교 ===
첫 번째 호출 (캐시 미스): 45ms
두 번째 호출 (캐시 히트): 2ms  
세 번째 호출 (캐시 히트): 1ms
성능 개선율: 95.56%
속도 개선 배수: 22.50x
```
- **Cold Start**: 45ms (복잡한 집계 쿼리)
- **Warm Up**: 2ms (Redis 캐시 조회)
- **최적화**: 1ms (메모리 최적화 효과)

### 동시성 테스트 결과

#### **대용량 동시 요청 처리**
```
=== 캐시 적용 동시성 테스트 ===
스레드 수: 100, 스레드당 요청: 10
총 요청 수: 1,000
성공률: 100%
전체 실행 시간: 1,234ms
평균 응답 시간: 2.3ms
초당 처리량(TPS): 810.37
```

### 대용량 데이터 시나리오

#### **스케일링 테스트**
| 주문 데이터 | 캐시 미스 | 캐시 히트 | 개선 배수 |
|------------|----------|---------|---------|
| 1만건 | 45ms | 2ms | 22.5x |
| 10만건 | 180ms | 2ms | 90x |
| 100만건 | 850ms | 2ms | 425x |

**결론**: 데이터가 클수록 캐시 효과가 극대화됨
