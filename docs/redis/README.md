# Redis 기반 시스템 설계 및 개발


### 구현 목표
- **인기상품 랭킹 시스템**: Redis SortedSet을 활용한 실시간 상품 랭킹 기능
- **비동기 쿠폰 발급 시스템**: Redis 다중 자료구조를 활용한 선착순 쿠폰 발급 기능

---

## STEP 13: 인기상품 랭킹 시스템 설계 및 구현

#### 1.1 자료구조 선택
```
Redis SortedSet (ZSET) 선택 이유:
- 자동 정렬 기능으로 O(log N) 성능
- 점수(판매량) 기반 순위 자동 계산
- 범위 조회로 Top N 효율적 추출
- ZINCRBY로 원자적 점수 증가
```

#### 1.2 시스템 아키텍처
```
주문 발생 → 파이프라인 업데이트 → 3일 데이터 집계 → Top5 조회
     ↓              ↓                   ↓             ↓
  OrderItem    Redis Pipeline      ZUNIONSTORE    ZREVRANGE
```

#### 2.1 파이프라인 기반 성능 최적화
```java
stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (OrderItem item : orderItems) {
        connection.zIncrBy(
            dailyKey.getBytes(),
            item.getQuantity(),
            item.getProductId().toString().getBytes()
        );
    }
    connection.expire(dailyKey.getBytes(), Duration.ofDays(EXPIRY_DAYS).getSeconds());
    return null;
});
```

#### 2.2 다중 날짜 데이터 집계
```java
// 최근 3일간 데이터 통합 집계
String[] dailyKeys = IntStream.range(0, 3)
    .mapToObj(today::minusDays)
    .map(date -> POPULAR_PRODUCTS_KEY_PREFIX + date.toString())
    .toArray(String[]::new);

// ZUNIONSTORE로 효율적 합집합
connection.zUnionStore(tempKey.getBytes(), keyBytes);
connection.zRevRangeWithScores(tempKey.getBytes(), 0, 4); // Top5 조회
```

---

## STEP 14: 비동기 쿠폰 발급 시스템 설계 및 구현

#### 1.1 다중 자료구조 전략
```
String (수량관리) + ZSet (대기열) + Set (발급완료) = 완전한 비동기 시스템
     ↓                 ↓              ↓
   DECR/INCR        ZADD/ZREM      SADD/SISMEMBER
  원자적 수량차감    나노초 타임스탬프   중복발급 방지
```

#### 1.2 3단계 발급 프로세스
```
1단계: 수량 차감 (reserveStockAsync)
   ↓
2단계: 대기열 진입 (enterQueueAsync) 
   ↓  
3단계: 발급 확정 (confirmIssuanceAsync)
```

#### 2.1 원자적 수량 관리
```java
// Redis DECR의 원자성 보장
Long remaining = stringRedisTemplate.opsForValue().decrement(countKey);
if (remaining < 0) {
    stringRedisTemplate.opsForValue().increment(countKey); // 롤백
    return false;
}
```
#### 2.2 트랜잭션 안전성
```java
// 예외 발생 시 단계별 롤백
try {
    return confirmIssuanceAsync(code, userId, policy);
} catch (Exception e) {
    removeFromQueueAsync(code, userId);    // 대기열 제거
    rollbackStockAsync(code);              // 수량 복구
    throw e;
}
```
---
