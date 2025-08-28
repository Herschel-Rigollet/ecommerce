# 분산 트랜잭션 설계 문서

### 1. 현재 아키텍처

```
OrderFacade.placeOrder() - 단일 트랜잭션
├── User 도메인 - 포인트 차감
├── Product 도메인 - 재고 차감  
├── Coupon 도메인 - 쿠폰 사용
└── Order 도메인 - 주문 생성
```

**장점:**
- ACID 속성 보장
- 구현 복잡도 낮음
- 데이터 일관성

**한계:**
- 도메인 간 강한 결합
- 확장성 제약
- 단일 장애점 위험

#### MSA 구조로 가려면?
```
User Service (독립 DB)     ←→ Order Service (독립 DB)
Product Service (독립 DB)  ←→ Coupon Service (독립 DB)
                           ↓
                  Data Platform (외부 시스템)
```

### 2. 도메인 분리 시 발생하는 트랜잭션 문제점

#### 데이터 일관성 문제

```java
1. User Service: 포인트 차감 성공 O
2. Product Service: 재고 차감 성공 O
3. Coupon Service: 쿠폰 사용 실패 X
4. Order Service: 주문 생성 불가 X

-> 포인트와 재고는 차감되었지만 주문은 생성되지 않음
```
- 타임아웃 발생 시 롤백 여부 판단 불가
- 분산 락 구현의 복잡성 증가
- 데드락 발생 가능성 증가
- 성능 저하 위험

### 3. Outbox Pattern을 활용한 트랜잭션 분리

```java
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private String eventId;
    
    @Column(nullable = false)
    private String aggregateId;  // 주문 ID 등
    
    @Column(nullable = false) 
    private String eventType;    // ORDER_CREATED, STOCK_RESERVED 등
    
    @Column(columnDefinition = "TEXT")
    private String eventPayload; // JSON 형태의 이벤트 데이터
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    private OutboxStatus status; // PENDING, PUBLISHED, FAILED
}

@Service
@Transactional
public class OrderServiceWithOutbox {
    
    public void createOrder(OrderRequest request) {
        // 1. 비즈니스 로직 실행
        Order order = new Order(request);
        orderRepository.save(order);
        
        // 2. 같은 트랜잭션에서 Outbox 이벤트 저장
        OutboxEvent event = OutboxEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(order.getId().toString())
            .eventType("ORDER_CREATED")
            .eventPayload(JsonUtil.toJson(order))
            .status(OutboxStatus.PENDING)
            .build();
            
        outboxRepository.save(event);
        
        // 3. 트랜잭션 커밋 시 둘 다 저장되거나 둘 다 실패
    }
}

@Component
@Scheduled(fixedDelay = 5000)
public class OutboxEventPublisher {
    
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
            .findByStatusAndCreatedAtBefore(
                OutboxStatus.PENDING, 
                LocalDateTime.now().minusMinutes(1)
            );
            
        for (OutboxEvent event : pendingEvents) {
            try {
                eventPublisher.publishEvent(createDomainEvent(event));
                event.markAsPublished();
                outboxRepository.save(event);
                
            } catch (Exception e) {
                event.markAsFailed();
                outboxRepository.save(event);
                log.error("이벤트 발행 실패: {}", event.getEventId());
            }
        }
    }
}
```

---

## Saga Pattern 적용 시 예상되는 문제점

#### 해당 패턴 도입 시 구조가 복잡해짐
```java
// 현재 구조
@Transactional
public OrderResponse placeOrder(OrderRequest request) {
    User user = userService.usePoint(userId, amount);
    List<Product> products = productService.decreaseStock(items);
    Coupon coupon = couponService.useCoupon(couponId);
    Order order = orderService.saveOrder(user, products, coupon);
}

// Saga Pattern 적용 시 복잡해지는 구조
@Component
public class OrderSagaOrchestrator {
    public void processOrder(OrderRequest request) {
        // 1단계: 주문 생성
        CreateOrderCommand createCmd = new CreateOrderCommand(request);
        sagaManager.executeStep("CREATE_ORDER", createCmd, this::compensateCreateOrder);
        
        // 2단계: 재고 예약
        ReserveStockCommand stockCmd = new ReserveStockCommand(request.getItems());
        sagaManager.executeStep("RESERVE_STOCK", stockCmd, this::compensateReserveStock);
        
        // 3단계: 포인트 차감
        UsePointCommand pointCmd = new UsePointCommand(request.getUserId(), amount);
        sagaManager.executeStep("USE_POINT", pointCmd, this::compensateUsePoint);
        
        // 4단계: 쿠폰 사용
        UseCouponCommand couponCmd = new UseCouponCommand(request.getCouponId());
        sagaManager.executeStep("USE_COUPON", couponCmd, this::compensateUseCoupon);
        
        // 5단계: 주문 확정
        ConfirmOrderCommand confirmCmd = new ConfirmOrderCommand(orderId);
        sagaManager.executeStep("CONFIRM_ORDER", confirmCmd, null);
    }
}
```

#### 데이터 일관성 저하
```java
// Saga 실행 중 중간 상태 노출
1. CREATE_ORDER: 주문 생성 완료 (PENDING 상태)
2. RESERVE_STOCK: 재고 차감 완료  
3. USE_POINT: 포인트 차감 실패
4. 보상 트랜잭션 시작

// 이 시점에서 다른 사용자가 조회하는 경우
- 주문은 존재하지만 결제되지 않음
- 재고는 차감되었지만 실제로 판매되지 않음

-> 사용자에게 일관되지 않은 정보 노출 위험
```

---

## 현재 시스템에서 발생 가능한 문제점

### 이벤트 유실 위험
```java
@Transactional
public OrderResponse placeOrder(OrderRequest request) {
    // 주문 처리 완료
    Order savedOrder = orderService.saveOrder(userId, orderItems);
    
    // 트랜잭션 커밋 완료
    
    // 이벤트 발행 (트랜잭션 외부에서 실행)
    eventPublisher.publishEvent(orderCompletedEvent);
    // 여기서 서버 장애 발생 시 이벤트 유실!
    
    return OrderResponse.from(savedOrder, orderItems);
}
```
**실제 발생 가능한 시나리오:**
1. 주문 데이터 DB 저장 성공
2. 트랜잭션 커밋 완료
3. 이벤트 발행 직전 서버 장애
4. **결과**: 주문은 완료되었지만 데이터 플랫폼에 전송되지 않음

---

## Outbox Pattern 적용 필요성

```java
// Outbox Pattern 적용 후
@Transactional
public OrderResponse placeOrder(OrderRequest request) {
    // 주문 처리
    Order savedOrder = orderService.saveOrder(userId, orderItems);
    
    // 같은 트랜잭션에서 Outbox 이벤트 저장
    OutboxEvent event = new OutboxEvent(
        "ORDER_COMPLETED", 
        savedOrder.getId(), 
        JsonUtils.toJson(orderData)
    );
    outboxRepository.save(event);
    
    // 둘 다 성공하거나 둘 다 실패 (원자성 보장)
    return OrderResponse.from(savedOrder, orderItems);
}
```

**해결되는 문제:**
- 이벤트 유실 완전 방지
- 트랜잭션 원자성 보장
- At-least-once 전송 보장
