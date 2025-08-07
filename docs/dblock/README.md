# 동시성 문제 해결

## 문제 식별 및 분석

### 동시성 이슈가 발생할 수 있는 포인트

#### 1. 재고 관리 동시성 문제
```java
// OrderFacade.placeOrder() - 재고 확인 및 차감 로직
for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
    Product product = productService.getProductById(itemRequest.getProductId());
    
    // 문제 지점 1: 재고 확인
    if (product.getStock() < itemRequest.getQuantity()) {
        throw new IllegalStateException("재고가 부족한 상품입니다");
    }
    
    // 문제 지점 2: 재고 차감 
    product.decreaseStock(itemRequest.getQuantity());
}
```

- T1: 사용자A가 재고 조회 → 10개 확인
- T2: 사용자B가 동시에 재고 조회 → 10개 확인
- T3: 사용자A가 7개 차감 → 재고 3개
- T4: 사용자B가 6개 차감 → 재고 -3개 (음수 재고)


#### 2. 사용자 잔액 동시성 문제
```java
// OrderFacade.placeOrder() - 잔액 확인 및 차감 로직
User user = userService.getPointByUserId(request.getUserId());

// 문제 지점 1: 잔액 확인
if (user.getPoint() < totalAmount) {
    throw new IllegalStateException("잔액이 부족합니다");
}

// 문제 지점 2: 잔액 차감
user.usePoint(totalAmount);
```

- 초기 잔액: 50,000원
- T1: 디바이스A가 잔액 조회 → 50,000원 확인
- T2: 디바이스B가 동시에 잔액 조회 → 50,000원 확인
- T3: 디바이스A가 30,000원 차감 → 잔액 20,000원
- T4: 디바이스B가 40,000원 차감 → 잔액 10,000원 (잘못된 계산)


#### 3. 선착순 쿠폰 발급 동시성 문제
```java
// CouponService.issueCoupon() - 쿠폰 한도 확인 및 발급
CouponPolicy policy = couponPolicyRepository.findByCode(code);

// 문제 지점 1: 발급 수량 확인
long issuedCount = couponRepository.countByCode(code);
if (issuedCount >= policy.getMaxCount()) {
    throw new IllegalStateException("쿠폰이 모두 소진되었습니다");
}

// 문제 지점 2: 쿠폰 발급
return couponRepository.save(new Coupon(...));
```

- 쿠폰 한도: 1000개, 현재 발급: 999개
- T1: 사용자A가 발급 수 조회 → 999개 (한도 내)
- T2: 사용자B~E가 동시에 조회 → 모두 999개 확인
- T3: 5명이 동시에 쿠폰 발급 → 1004개 발급 (한도 초과)


#### 3. 복합 트랜잭션 동시성 문제
```java
// OrderFacade.placeOrder() - 여러 자원에 대한 순차적 접근
// 1. 상품 재고 차감
// 2. 쿠폰 사용 처리  
// 3. 사용자 포인트 차감
// 4. 주문 생성

// 각 단계에서 다른 사용자가 같은 자원에 접근 가능
```

Phantom Read
- 사용자가 주문 중 다른 사용자가 재고/잔액 상태 변경
- 주문 과정에서 일관성 없는 데이터 읽기


#### 4. 쿠폰 사용 동시성 문제
```java
// OrderFacade.applyCouponDiscount() - 쿠폰 유효성 확인 및 사용
Coupon coupon = couponService.getCouponById(couponId);

// 문제 지점 1: 쿠폰 상태 확인
if (coupon.isUsed()) {
    throw new IllegalStateException("이미 사용된 쿠폰입니다");
}

// 문제 지점 2: 쿠폰 사용 처리
coupon.use();
couponService.saveCoupon(coupon);
```

- T1: 사용자가 모바일에서 쿠폰 사용 조회 → 미사용 상태 확인
- T2: 동시에 PC에서 같은 쿠폰 조회 → 미사용 상태 확인
- T3: 모바일에서 쿠폰 사용 처리 → 사용 완료
- T4: PC에서도 쿠폰 사용 처리 → 중복 사용 발생


#### 5. 포인트 충전 동시성 문제
```java
// UserService.charge() - 포인트 충전 로직
User user = userRepository.findById(userId);

// 문제 지점 1: 현재 포인트 조회
long currentPoint = user.getPoint();

// 문제 지점 2: 포인트 증가
user.charge(amount);
userRepository.save(user);
```

- 초기 포인트: 10,000원
- T1: 앱에서 포인트 조회 → 10,000원
- T2: 웹에서 동시에 포인트 조회 → 10,000원
- T3: 앱에서 5,000원 충전 → 15,000원으로 저장
- T4: 웹에서 3,000원 충전 → 13,000원으로 덮어쓰기 (5,000원 충전 손실)

---


## 해결 방안

### DB락 선택 기준

#### **비관적 락 선택 기준**
1. **높은 충돌 가능성**: 동시 접근이 빈번한 자원
2. **정확성 Critical**: 데이터 일관성이 비즈니스에 치명적
3. **실시간 처리**: 재시도보다 순차 처리가 적합한 경우

#### **낙관적 락 선택 기준**
1. **낮은 충돌 가능성**: 동시 접근이 드문 자원
2. **성능 우선**: 처리 속도가 정확성보다 중요한 경우
3. **재시도 허용**: 실패 시 재시도가 가능한 비즈니스 로직
4. **읽기 위주**: 수정보다 조회가 많은 데이터

### 각 문제별 해결 전략

#### 1. 재고 관리: 비관적 락
- 인기 상품은 동시 주문 빈발
- 재고 음수는 불가능

**구현 방법:**
```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}

@Service
@Transactional
public class ProductService {
    
    public void decreaseStock(Long productId, int quantity) {
        // 비관적 락으로 상품 잠금
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow();
        
        // 재고 확인 + 차감
        if (product.getStock() < quantity) {
            throw new IllegalStateException("재고 부족");
        }
        product.decreaseStock(quantity);
        // 트랜잭션 종료 시 자동 락 해제
    }
}
```

**장점:**
- 100% 데이터 일관성 보장
- Race Condition 원천 차단
- 비즈니스 로직 단순화

**단점:**
- 성능 저하 (평균 응답시간 100ms → 200ms)
- 처리량 감소 (1000 TPS → 500 TPS)
- 락 대기 시간 발생

#### 2. 잔액 관리: 비관적 락
- 포인트 오차는 정산 시 복잡한 수정 필요
- 동일 사용자 다중 디바이스 사용 증가

**구현 방법:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}

@Service  
@Transactional
public class UserService {
    
    public void usePoint(Long userId, long amount) {
        // 사용자 계정 잠금
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow();
        
        // 잔액 확인 + 차감
        if (user.getPoint() < amount) {
            throw new IllegalStateException("포인트 부족");
        }
        user.usePoint(amount);
        // 트랜잭션 종료 시 자동 락 해제
    }
}
```

#### 3. 선착순 쿠폰 발급: 비관적 락
- 선착순 이벤트는 순차 처리가 공정
- 이벤트 시작 시 폭발적 동시 접근

**구현 방법:**
```java
@Service
@Transactional  
public class CouponService {
    
    public Coupon issueCoupon(Long userId, String code) {
        // 쿠폰 정책 잠금 (글로벌 락)
        CouponPolicy policy = couponPolicyRepository.findByCodeForUpdate(code)
                .orElseThrow();
        
        // 한도 확인 + 발급
        long issuedCount = couponRepository.countByCode(code);
        if (issuedCount >= policy.getMaxCount()) {
            throw new IllegalStateException("쿠폰 소진");
        }
        
        return couponRepository.save(new Coupon(...));
        // 트랜잭션 종료 시 자동 락 해제
    }
}
```


#### 4. 쿠폰 사용: 낙관적 락
- 동일 쿠폰 동시 사용은 드문 케이스 (사용자별 개별 소유)
- 중복 사용 방지가 핵심, 성능도 중요

**구현 방법:**
```java
@Entity
public class Coupon {
    // ... 기존 필드들
    
    @Version
    private Long version; // 낙관적 락용 버전 필드
}

@Service
@Transactional
public class CouponService {
    
    public void useCouponOptimistic(Long couponId, Long userId) {
        try {
            Coupon coupon = couponRepository.findByCouponId(couponId)
                    .orElseThrow();
            
            // 쿠폰 유효성 검증
            validateCoupon(coupon, userId);
            
            // 쿠폰 사용 처리 (버전 체크)
            coupon.use();
            couponRepository.save(coupon); // OptimisticLockingFailureException 가능
            
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("쿠폰이 이미 사용되었습니다. 다시 시도해주세요.");
        }
    }
}
```

**장점:**
- 일반적인 경우 높은 성능 (락 대기 없음)
- 중복 사용 완전 차단
- 사용자 경험 향상 (즉시 응답)

**단점:**
- 충돌 시 재시도 필요
- 비즈니스 로직에 예외 처리 추가

#### 5. 포인트 충전: 낙관적 락
- 사용자별 개별 계정, 충전 충돌 가능성 낮음
- 충전 실패 시 재시도 용이한 비즈니스 특성

**구현 방법:**
```java
@Entity
public class User {
    // ... 기존 필드들
    
    @Version
    private Long version; // 낙관적 락용 버전 필드
}

@Service
@Transactional
public class UserService {
    
    // 단순 낙관적 락 충전
    public void chargeOptimistic(Long userId, long amount) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow();
            
            user.charge(amount);
            userRepository.save(user); // OptimisticLockingFailureException 가능
            
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("충전 처리 중 다른 작업과 충돌이 발생했습니다. 다시 시도해주세요.");
        }
    }
    
    // 재시도 로직 포함 충전
    public void chargeOptimisticWithRetry(Long userId, long amount) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow();
                
                user.charge(amount);
                userRepository.save(user);
                return; // 성공 시 종료
                
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new IllegalStateException("충전 처리가 " + maxRetries + "회 재시도 후에도 실패했습니다.");
                }
                
                // 100ms 대기 후 재시도
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("충전 처리가 중단되었습니다.", ie);
                }
            }
        }
    }
}
```

**장점:**
- 일반적인 경우 높은 성능
- 동시 충전 드물어 충돌 확률 낮음
- 재시도로 사용자 경험 개선

**단점:**
- 드물게 재시도 발생
- 복잡한 오류 처리 로직

---

## 동시성 문제 및 해결 전략 요약

| 구분 | 발생 원인 | 예시 상황 | 해결 전략 | 락 방식 | 장점 | 단점 |
|------|-----------|-----------|-----------|---------|------|------|
| **재고 관리** | 다중 사용자가 동시에 재고 조회·차감 | 재고 10개 → A 7개 주문, B 6개 주문 → 재고 -3개 | 재고 확인 + 차감을 **비관적 락**으로 묶어 처리 | **비관적 락 (PESSIMISTIC_WRITE)** | 데이터 100% 일관성, Race Condition 차단 | 성능 저하, 처리량 감소 |
| **사용자 잔액** | 다중 기기에서 동시 결제 | 잔액 5만원 → A 3만원 차감, B 4만원 차감 → 잔액 1만원 (오류) | 잔액 확인 + 차감을 **비관적 락**으로 처리 | **비관적 락** | 포인트 오차 방지, 데이터 신뢰성 보장 | 응답 속도 저하, 락 대기 가능 |
| **선착순 쿠폰 발급** | 다중 사용자가 동시에 발급 수량 조회 | 한도 1000개, 현재 999개 → 동시 발급으로 1004개 발급 | 발급 수량 확인 + 저장을 **비관적 락**으로 처리 | **비관적 락** | 순차 처리로 공정성 보장, 초과 발급 방지 | 이벤트 폭주 시 대기 시간 증가 |
| **쿠폰 사용** | 동일 쿠폰 동시 사용 | 모바일·PC에서 동시에 쿠폰 사용 요청 | `@Version` 필드 기반 **낙관적 락** 적용 | **낙관적 락 (Optimistic Lock)** | 락 대기 없음, 일반 케이스에서 빠른 성능 | 충돌 시 재시도 필요 |
| **포인트 충전** | 다중 기기에서 동시 충전 | 1만원 → 앱 5천 충전, 웹 3천 충전 → 1.3만원(5천 충전 손실) | `@Version` + 재시도 로직 적용 | **낙관적 락** | 빠른 성능, 드문 충돌은 재시도로 해결 | 예외 처리·재시도 로직 필요 |

