# k6 성능 테스트 설계 및 결과

## 1. 선착순 쿠폰

**동시성 이슈**:
- 다수가 동시에 같은 쿠폰 요청
- Race Condition 발생 가능성
- 데이터 일관성 보장 필요

**정확성 요구사항**:
- 발급 수량 초과 금지
- 중복 발급 방지
- 트랜잭션 무결성 보장

**성능 요구사항**:
- 순간적 대용량 트래픽 처리
- 빠른 응답 시간
- 시스템 안정성 유지

### DB락/Redis/Kafka
- **DB 락**: 강한 일관성 vs 낮은 성능
- **Redis**: 높은 성능 vs 복잡성
- **Kafka**: 최고 확장성 vs 최종 일관성

## 2. 테스트 시나리오 설계

### DB 락 방식 테스트 설계

```javascript
// 락 경합 상황 집중 테스트
const dbLockScenarios = {
  lockContention: {
    // 동일 쿠폰에 집중 요청
    vus: 100,
    sameTarget: true,
    purpose: '락 대기 시간 측정'
  },
  
  deadlockSimulation: {
    // 복수 쿠폰 동시 요청
    multipleTargets: true,
    purpose: '데드락 발생 가능성 확인'
  },
  
  timeoutHandling: {
    // 긴 대기 시간 설정
    lockTimeout: '10s',
    purpose: '타임아웃 처리 검증'
  }
};
```

#### 검증 포인트
- 분산락 대기 시간
- 낙관적 락 충돌 빈도
- 데드락 발생률
- 트랜잭션 롤백 비율

### Redis 비동기 방식 테스트 설계

```javascript
const redisAsyncScenarios = {
  queueProcessing: {
    // 대기열 처리 성능
    massiveEnqueue: true,
    batchProcessing: true,
    purpose: '대기열 처리 효율성'
  },
  
  statusPolling: {
    // 상태 조회 부하
    pollingInterval: '2s',
    concurrentPolling: 300,
    purpose: '실시간 상태 조회 부하'
  },
  
  redisFailover: {
    // Redis 장애 시나리오
    simulateFailure: true,
    fallbackTest: true,
    purpose: '장애 복구 능력'
  }
};
```

#### 검증 포인트
- 대기열 진입 속도
- 비동기 처리 완료 시간
- 상태 조회 정확성
- Redis 메모리 사용량

### Kafka 이벤트 방식 테스트 설계

#### 특화 시나리오
```javascript
const kafkaEventScenarios = {
  massivePublishing: {
    // 대량 이벤트 발행
    eventRate: '1000/s',
    partitionStrategy: 'userId',
    purpose: '이벤트 발행 처리량'
  },
  
  orderGuarantee: {
    // 순서 보장 테스트
    sequentialEvents: true,
    samePartition: true,
    purpose: '이벤트 순서 보장 확인'
  },
  
  brokerFailover: {
    // Kafka 장애 시나리오
    brokerShutdown: true,
    rebalancing: true,
    purpose: '브로커 장애 시 복구'
  }
};
```

#### 검증 포인트
- 이벤트 발행 처리량
- 컨슈머 처리 지연시간
- 파티션 밸런싱
- 메시지 순서 보장


## 3. 예상 결과 및 성공 기준

#### DB 락 방식
```
예상 처리량: 50-100 TPS
예상 응답시간: 1-5초
장점: 100% 정확성 보장
단점: 낮은 처리량, 높은 지연시간
```

#### Redis 비동기 방식
```
예상 처리량: 500-1000 TPS
예상 응답시간: 100-500ms
장점: 빠른 응답, 사용자 경험
단점: 복잡한 구현, 인프라 의존성
```

#### Kafka 이벤트 방식
```
예상 처리량: 1000+ TPS
예상 응답시간: 50-200ms
장점: 최고 확장성, 시스템 분리
단점: 최종 일관성, 높은 복잡도
```

## 4. 결과
- 응답속도(p95)는 양호하나, 실패율이 비정상적으로 높음....

### DB 락
<img width="1580" height="735" alt="Image" src="https://github.com/user-attachments/assets/8c71cb90-7dda-4ff4-afc8-f1211adbc225" />

### Redis
<img width="1585" height="728" alt="Image" src="https://github.com/user-attachments/assets/0ac7d2bf-0e4b-48d2-8030-a8d511c70da3" />

### Kafka
<img width="1580" height="739" alt="Image" src="https://github.com/user-attachments/assets/950eeb20-6ed7-4f8f-a395-e4b7373b037f" />

### 테스트 구성
- **도구/구성**: k6(로컬) → InfluxDB v1.8 → Grafana(InfluxQL)
- **시나리오**:
    - DB 락 기반 동기 발급
    - 낙관적 락 발급
    - Redis 비동기 큐 발급
    - Kafka 이벤트 발급 및 결과 폴링
- **대시보드 지표**: RPS, 실패율, p95/avg, VUs, 커스텀 메트릭

---

- **실패율 (http_req_failed)**: 최대 100% (대부분의 요청 실패)
- **응답시간 (p95)**: 200~300ms 수준 (빠른 실패 가능성)
- **커스텀 체크/임계치**:
    - `async request status 202`: 0%
    - `async request has requestId`: 0%
    - `status polling valid data`: 0%
    - `redis_queue_entry_success`: 0
- **InfluxDB**: `max-series-per-database limit exceeded` → 메트릭 일부 드롭 발생

---

## 추정 원인

### API 계약 불일치
- 비동기/이벤트 API가 **202 + requestId** 또는 **200 + status/queuePosition**을 반환하지 않음
- JSON 키 불일치로 k6 체크 전부 실패

### 백엔드 처리 미완성
- Redis 큐 등록 후 상태 미저장 → 폴링 시 항상 기본값
- Kafka 발행 후 결과 저장소 미기록 → 결과 조회 실패

---

## 개선안

- **API 계약 고정**
    - 발급: 202 + `{"data":{"requestId":"..."}}`
    - 상태 조회: 200 + `{"data":{"status":"WAITING|COMPLETED","queuePosition":0}}`
    - Kafka 결과: 200 + `{"data":{"processed":true,"success":true}}` (미처리시 404 허용)

- **Redis/Kafka 상태 기록 보강**
    - 큐 진입 시 `status=WAITING` 저장
    - 워커가 처리 단계별 상태 업데이트
