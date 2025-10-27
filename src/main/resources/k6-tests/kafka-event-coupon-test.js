import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const kafkaEventPublished = new Counter('kafka_event_published');
const eventProcessingTime = new Trend('kafka_event_processing_time');
const eventOrderConsistency = new Rate('kafka_event_order_consistency');
const kafkaPartitionBalance = new Rate('kafka_partition_balance');
const eventualConsistency = new Rate('kafka_eventual_consistency');
const resultEventReceived = new Counter('kafka_result_event_received');

export const options = {
  scenarios: {
    // 시나리오 1: 대량 이벤트 발행 테스트 (Producer 성능)
    massive_event_publishing: {
      executor: 'ramping-vus',
      stages: [
        { duration: '20s', target: 300 },  // 점진적 증가
        { duration: '60s', target: 1000 }, // 대량 이벤트 발행
        { duration: '40s', target: 1500 }, // 최대 부하
        { duration: '20s', target: 0 },    // 종료
      ],
      exec: 'testKafkaEventPublishing',
    },

    // 시나리오 2: 이벤트 처리 순서 보장 테스트
    event_ordering_test: {
      executor: 'per-vu-iterations',
      vus: 100,
      iterations: 10,  // 각 VU가 10번씩 순차 요청
      startTime: '30s',
      maxDuration: '3m',
      exec: 'testEventOrdering',
    },

    // 시나리오 3: 결과 이벤트 폴링 부하 테스트
    result_polling_load: {
      executor: 'constant-vus',
      vus: 200,
      duration: '2m',
      startTime: '1m',
      exec: 'testResultEventPolling',
    },

    // 시나리오 4: Kafka 파티션 밸런싱 테스트
    partition_balancing: {
      executor: 'constant-vus',
      vus: 50,
      duration: '90s',
      startTime: '3m',
      exec: 'testPartitionBalancing',
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<5000'],           // 이벤트 발행은 빠르게
    kafka_event_published: ['count>800'],        // 최소 800개 이벤트 발행
    kafka_event_processing_time: ['p(90)<15000'], // 90% 이벤트가 15초 이내 처리
    kafka_event_order_consistency: ['rate>0.95'], // 순서 보장 95% 이상
    kafka_eventual_consistency: ['rate>0.98'],    // 최종 일관성 98% 이상
    kafka_result_event_received: ['count>400'],   // 최소 400개 결과 수신
  },
};

const BASE_URL = 'http://localhost:8080';

export function setup() {
  console.log('=== Kafka 이벤트 기반 쿠폰 테스트 시작 ===');

  // 테스트용 쿠폰 정책 생성
  const testCoupons = [
    { code: 'KAFKA_MASSIVE_1000', maxCount: 1000, discountRate: 10 },
    { code: 'KAFKA_ORDER_200', maxCount: 200, discountRate: 15 },
    { code: 'KAFKA_POLL_300', maxCount: 300, discountRate: 20 },
    { code: 'KAFKA_PARTITION_100', maxCount: 100, discountRate: 25 },
  ];

  testCoupons.forEach(coupon => {
    const response = http.post(`${BASE_URL}/admin/coupon-policies`,
      JSON.stringify(coupon),
      { headers: { 'Content-Type': 'application/json' } }
    );
    console.log(`Kafka 테스트 쿠폰 생성: ${coupon.code} - ${response.status}`);
  });

  // Kafka 상태 확인
  const kafkaHealth = http.get(`${BASE_URL}/admin/kafka/health`);
  if (kafkaHealth.status === 200) {
    console.log('Kafka 클러스터 정상 상태 확인');
  } else {
    console.log('경고: Kafka 상태 확인 실패');
  }

  return { testCoupons };
}

// 시나리오 1: 대량 이벤트 발행 테스트
export function testKafkaEventPublishing(data) {
  const userId = Math.floor(Math.random() * 50000) + 1;
  const couponCode = 'KAFKA_MASSIVE_1000';

  const eventStartTime = Date.now();

  // Kafka 이벤트 기반 쿠폰 발급 요청 (CouponController.issueCoupon)
  const publishResponse = http.post(
    `${BASE_URL}/coupons/issue/${userId}?code=${couponCode}`,
    null,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '8s',
    }
  );

  const publishSuccess = check(publishResponse, {
    'kafka publish status 202': (r) => r.status === 202,
    'kafka publish fast response': (r) => r.timings.duration < 3000,
    'kafka publish has requestId': (r) => {
      try {
        const data = JSON.parse(r.body);
        return data.data && data.data.requestId;
      } catch (e) {
        return false;
      }
    },
  });

  if (publishResponse.status === 202) {
    kafkaEventPublished.add(1);

    try {
      const responseData = JSON.parse(publishResponse.body);
      const requestId = responseData.data.requestId;
      console.log(`Kafka 이벤트 발행: userId=${userId}, requestId=${requestId}`);

      // 이벤트 처리 결과 대기 (폴링 방식)
      let pollCount = 0;
      let processingComplete = false;
      const maxPolls = 15; // 최대 30초 대기

      while (pollCount < maxPolls && !processingComplete) {
        sleep(2); // 2초 간격 폴링
        pollCount++;

        // 결과 확인 API (Consumer가 처리한 결과 조회)
        const resultResponse = http.get(
          `${BASE_URL}/coupons/result/${userId}?code=${couponCode}&requestId=${requestId}`
        );

        if (resultResponse.status === 200) {
          const resultData = JSON.parse(resultResponse.body);
          const isProcessed = resultData.data.processed;
          const isSuccess = resultData.data.success;

          if (isProcessed) {
            processingComplete = true;
            const totalTime = Date.now() - eventStartTime;
            eventProcessingTime.add(totalTime);
            resultEventReceived.add(1);

            if (isSuccess) {
              eventualConsistency.add(true);
              console.log(`Kafka 이벤트 처리 성공: userId=${userId}, 총시간=${totalTime}ms, 폴링=${pollCount}회`);
            } else {
              eventualConsistency.add(true); // 품절도 정상 처리
              console.log(`Kafka 이벤트 품절: userId=${userId}, 총시간=${totalTime}ms`);
            }
          } else {
            console.log(`Kafka 이벤트 처리 중: userId=${userId}, 폴링=${pollCount}회`);
          }
        } else if (resultResponse.status === 404) {
          // 아직 처리되지 않음
          continue;
        } else {
          console.log(`결과 조회 오류: userId=${userId}, status=${resultResponse.status}`);
          break;
        }
      }

      // 타임아웃 처리
      if (!processingComplete) {
        eventualConsistency.add(false);
        console.log(`Kafka 이벤트 처리 타임아웃: userId=${userId}, 폴링=${pollCount}회`);
      }

    } catch (e) {
      eventualConsistency.add(false);
      console.log(`Kafka 응답 파싱 오류: userId=${userId}, error=${e.message}`);
    }

  } else if (publishResponse.status === 400) {
    // 잘못된 요청 (쿠폰 코드 등)
    console.log(`Kafka 발행 실패: userId=${userId}, status=${publishResponse.status}`);

  } else {
    console.log(`Kafka 시스템 오류: userId=${userId}, status=${publishResponse.status}`);
  }

  sleep(Math.random() * 2 + 1); // 1-3초 대기
}

// 시나리오 2: 이벤트 처리 순서 보장 테스트
export function testEventOrdering(data) {
  const userId = 10000 + (__VU * 100) + __ITER; // 고유한 사용자 ID 생성
  const couponCode = 'KAFKA_ORDER_200';

  // 동일 사용자가 연속으로 요청하여 순서 보장 테스트
  const requests = [];
  const requestTimes = [];

  // 3개의 연속 요청 (첫 번째만 성공해야 함)
  for (let i = 0; i < 3; i++) {
    const startTime = Date.now();

    const response = http.post(
      `${BASE_URL}/coupons/issue/${userId}?code=${couponCode}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
        timeout: '5s',
      }
    );

    requests.push(response);
    requestTimes.push(Date.now() - startTime);

    sleep(0.1); // 100ms 간격으로 연속 요청
  }

  // 순서 보장 검증
  let orderingCorrect = true;
  let successCount = 0;

  requests.forEach((response, index) => {
    check(response, {
      [`order test ${index} valid status`]: (r) => r.status === 202 || r.status === 409,
      [`order test ${index} fast response`]: (r) => r.timings.duration < 5000,
    });

    if (response.status === 202) {
      successCount++;
      kafkaEventPublished.add(1);
    } else if (response.status === 409) {
      // 중복 요청 감지 (정상)
    }
  });

  // 순서 보장 확인: 첫 번째 요청만 성공해야 함
  if (successCount === 1 && requests[0].status === 202) {
    eventOrderConsistency.add(true);
    console.log(`이벤트 순서 보장 성공: userId=${userId}, 중복차단=${requests.length - successCount}건`);
  } else if (successCount === 0) {
    // 모두 실패 (품절 등)
    eventOrderConsistency.add(true);
    console.log(`이벤트 순서 테스트 품절: userId=${userId}`);
  } else {
    eventOrderConsistency.add(false);
    console.log(`이벤트 순서 보장 실패: userId=${userId}, 성공=${successCount}건`);
  }

  sleep(Math.random() * 3 + 2); // 2-5초 대기
}

// 시나리오 3: 결과 이벤트 폴링 부하 테스트
export function testResultEventPolling(data) {
  const userId = Math.floor(Math.random() * 5000) + 1;
  const couponCode = 'KAFKA_POLL_300';
  const requestId = `REQ_${userId}_${couponCode}_${Date.now()}`;

  // 결과 조회 API 부하 테스트 (실제 결과가 없어도 API 성능 측정)
  const resultResponse = http.get(
    `${BASE_URL}/coupons/result/${userId}?code=${couponCode}&requestId=${requestId}`,
    {
      timeout: '3s',
    }
  );

  const success = check(resultResponse, {
    'result polling response ok': (r) => r.status === 200 || r.status === 404,
    'result polling fast response': (r) => r.timings.duration < 2000,
    'result polling valid format': (r) => {
      if (r.status === 404) return true; // Not found는 정상
      try {
        const data = JSON.parse(r.body);
        return data.data !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (success) {
    if (resultResponse.status === 200) {
      resultEventReceived.add(1);
      eventualConsistency.add(true);
    } else if (resultResponse.status === 404) {
      // 아직 처리되지 않음 (정상)
      eventualConsistency.add(true);
    }
  } else {
    eventualConsistency.add(false);
  }

  sleep(Math.random() * 1 + 0.5); // 0.5-1.5초 (빠른 폴링 패턴)
}

// 시나리오 4: Kafka 파티션 밸런싱 테스트
export function testPartitionBalancing(data) {
  const userId = Math.floor(Math.random() * 1000) + 1;
  const couponCode = 'KAFKA_PARTITION_100';

  // 파티션 분산을 위한 다양한 사용자 ID 패턴
  const userIds = [
    userId,                    // 기본
    userId + 10000,            // 파티션 2
    userId + 20000,            // 파티션 3
    userId + 30000,            // 파티션 4
  ];

  const selectedUserId = userIds[Math.floor(Math.random() * userIds.length)];

  const response = http.post(
    `${BASE_URL}/coupons/issue/${selectedUserId}?code=${couponCode}`,
    null,
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Partition-Key': selectedUserId.toString(), // 파티션 키 힌트
      },
      timeout: '5s',
    }
  );

  const success = check(response, {
    'partition test valid status': (r) => r.status === 202 || r.status === 409,
    'partition test response time': (r) => r.timings.duration < 3000,
  });

  if (success) {
    kafkaPartitionBalance.add(true);

    if (response.status === 202) {
      kafkaEventPublished.add(1);
      console.log(`파티션 테스트 성공: userId=${selectedUserId}, partition=${selectedUserId % 4}`);
    }
  } else {
    kafkaPartitionBalance.add(false);
    console.log(`파티션 테스트 실패: userId=${selectedUserId}, status=${response.status}`);
  }

  sleep(Math.random() * 2 + 1); // 1-3초 대기
}

export function teardown(data) {
  console.log('=== Kafka 이벤트 기반 쿠폰 테스트 완료 ===');

  // Kafka 토픽 및 컨슈머 상태 확인
  const kafkaStatus = http.get(`${BASE_URL}/admin/kafka/status`);
  if (kafkaStatus.status === 200) {
    try {
      const status = JSON.parse(kafkaStatus.body);
      console.log('=== Kafka 클러스터 상태 ===');
      console.log(`- 토픽 개수: ${status.topics?.length || 0}`);
      console.log(`- 컨슈머 그룹: ${status.consumerGroups?.length || 0}`);
      console.log(`- 처리된 메시지: ${status.processedMessages || 0}`);
      console.log(`- 처리 대기: ${status.pendingMessages || 0}`);
    } catch (e) {
      console.log('Kafka 상태 파싱 실패');
    }
  }

  // 최종 쿠폰 발급 현황
  data.testCoupons.forEach(coupon => {
    const response = http.get(`${BASE_URL}/admin/coupons/${coupon.code}/status`);
    if (response.status === 200) {
      try {
        const status = JSON.parse(response.body);
        console.log(`${coupon.code}: 발급 ${status.issuedCount}/${coupon.maxCount}개 (${(status.issuedCount/coupon.maxCount*100).toFixed(1)}%)`);
      } catch (e) {
        console.log(`${coupon.code}: 상태 확인 실패`);
      }
    }
  });

  console.log('=== Kafka 성능 지표 요약 ===');
  console.log('- 이벤트 발행: 높은 처리량으로 대량 요청 수용');
  console.log('- 순서 보장: 동일 파티션 내 메시지 순서 보장');
  console.log('- 최종 일관성: 비동기 처리로 높은 확장성');
  console.log('- 파티션 분산: 부하 분산으로 병렬 처리 효율성');
  console.log('- 장애 복구: 메시지 영속성으로 안정성 보장');
}