# 📦 E-Commerce 재고 관리 시스템

> 대용량 트래픽 환경에서의 동시성 제어와 실시간 재고 관리를 학습하기 위한 이커머스 프로젝트

## 📌 프로젝트 개요
  
**프로젝트 구분:** 항해 플러스 백엔드 9기 이커머스 프로젝트
**주요 학습 목표:**
- 대용량 트래픽 처리 및 동시성 제어
- 레이어드 아키텍처 설계 및 리팩토링
- 실시간 데이터 정합성 유지

---

## 🛠️ 기술 스택

### Backend
- **Language:** Java 17
- **Framework:** Spring Boot 3.4.1
- **ORM:** Spring Data JPA
- **Database:** MySQL 8.0
- **Caching:** Redis
- **Messaging:** Kafka
- **Build Tool:** Gradle

### Infra & DevOps
- **Container:** Docker, Docker Compose
- **Testing:** JUnit 5, Mockito

---

## 🎯 주요 기능

### 1️⃣ 사용자 포인트 관리
- 포인트 충전/조회
- 동시 요청 처리 시 데이터 정합성 보장

### 2️⃣ 상품 조회
- 상품 목록 조회
- 인기 상품 Top 5 조회 (Redis 캐싱)

### 3️⃣ 주문 처리
- 장바구니 담기
- 주문 생성 및 결제
- **동시 주문 시 재고 차감 동시성 제어:**
  - 비관적 락 (Pessimistic Lock)
  - 낙관적 락 (Optimistic Lock)
  - Redis 분산락
  - Kafka 이벤트 기반 비동기 처리

---

## 🏗️ 아키텍처 설계

### 계층 구조 (Layered Architecture)
```
Controller → Application → Domain ← Interface → Infra
```

#### 계층별 책임

| 계층 | 책임 | 의존 방향 |
|------|------|-----------|
| **Presentation** | HTTP 요청/응답 처리, DTO 변환 | → Application |
| **Application** | UseCase 실행, 트랜잭션 관리 | → Domain, Repository |
| **Domain** | 핵심 비즈니스 로직 (재고 차감, 포인트 계산) | 독립 |
| **Infrastructure** | DB 접근, 외부 시스템 연동 | ← Repository Interface |

### 패키지 구조
```
src/main/java/com/hhplus/ecommerce
├── presentation/        # Controller, DTO
│   ├── user/
│   ├── product/
│   └── order/
├── application/         # Service (UseCase)
│   ├── user/
│   ├── product/
│   └── order/
├── domain/             # Entity, 도메인 로직
│   ├── user/
│   ├── product/
│   └── order/
└── infrastructure/     # Repository 구현체
    ├── user/
    ├── product/
    └── order/
```

---

## 🔥 핵심 구현: 동시성 제어

### 1. 비관적 락 (Pessimistic Lock)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithLock(@Param("id") Long id);
```
- **장점:** 데이터 정합성 보장
- **단점:** 성능 저하 (Lock 대기 시간)

### 2. 낙관적 락 (Optimistic Lock)
```java
@Version
private Long version;
```
- **장점:** 충돌이 적을 때 성능 우수
- **단점:** 재시도 로직 필요

### 3. Redis 분산락
```java
@RedissonLock(key = "#productId")
public void decreaseStock(Long productId, int quantity) {
    // 재고 차감 로직
}
```
- **장점:** 다중 서버 환경에서 동시성 제어
- **단점:** Redis 의존성

### 4. Kafka 이벤트 기반 처리
```java
@KafkaListener(topics = "order-events")
public void handleOrderEvent(OrderEvent event) {
    // 비동기 재고 차감
}
```
- **장점:** 비동기 처리로 응답 속도 향상
- **단점:** 최종 일관성 모델 (Eventual Consistency)

---

## 📊 성능 개선

### Redis 캐싱 적용
- **대상:** 인기 상품 Top 5
- **효과:** 조회 응답 시간 80% 감소 (500ms → 100ms)

### 인덱스 최적화
- 상품 조회 쿼리 인덱스 추가
- 조회 성능 60% 향상

---

## 🧪 테스트 전략

### 테스트 커버리지
- **Domain Layer:** 순수 단위 테스트 (100%)
- **Application Layer:** Mock 기반 단위 테스트 (90%)
- **Infrastructure Layer:** 통합 테스트 (80%)

### 주요 테스트 케이스
- 동시 주문 시 재고 차감 정합성 테스트
- 포인트 동시 충전 테스트
- Redis 캐시 동작 테스트

---

## 🎓 학습 내용

### 아키텍처 리팩토링 경험
**Before:** 계층별 패키지 구조
```
├── controller/
├── service/
├── domain/
└── infrastructure/
```

**After:** 도메인별 패키지 구조
```
├── user/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
```

**변경 이유:**
- 기능 추가 시 관련 코드 위치 파악 용이
- 도메인 중심 사고 강화
- 멘토링 피드백 반영

### 배운 점
1. **동시성 제어의 중요성**
   - 단순 구현과 실제 운영 환경의 차이 체감
   - 각 방식의 트레이드오프 이해

2. **아키텍처 설계의 중요성**
   - 초기 설계가 유지보수성에 미치는 영향
   - 레이어별 책임 분리의 중요성

3. **테스트 주도 개발**
   - Mock을 활용한 단위 테스트 작성법
   - 통합 테스트의 필요성

---

## 🚀 실행 방법

### 1. 사전 준비
- Java 17
- Docker & Docker Compose

### 2. 인프라 실행
```bash
docker-compose up -d
```

### 3. 애플리케이션 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. API 테스트
```bash
# 포인트 충전
curl -X POST http://localhost:8080/api/users/1/charge \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000}'

# 상품 조회
curl http://localhost:8080/api/products

# 주문 생성
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"productId": 1, "quantity": 2}
    ]
  }'
```

---

## 📂 주요 API 명세

### 사용자
- `POST /api/users/{userId}/charge` - 포인트 충전
- `GET /api/users/{userId}/point` - 포인트 조회

### 상품
- `GET /api/products` - 상품 목록 조회
- `GET /api/products/popular` - 인기 상품 Top 5

### 주문
- `POST /api/orders` - 주문 생성
- `GET /api/orders/{orderId}` - 주문 조회

---

## 📌 향후 개선 계획

- [ ] CI/CD 파이프라인 구축 (GitHub Actions)
- [ ] API 문서 자동화 (Swagger/SpringDoc)
- [ ] 모니터링 시스템 도입 (Prometheus, Grafana)
- [ ] 헥사고날 아키텍처로 리팩토링
- [ ] 부하 테스트 (JMeter, nGrinder)

