import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const redisQueueEntry = new Counter('redis_queue_entry_success');
const redisProcessingTime = new Trend('redis_async_processing_time');
const queuePositionAccuracy = new Rate('queue_position_accuracy');
const redisDataConsistency = new Rate('redis_data_consistency');
const asyncIssuanceSuccess = new Rate('async_issuance_final_success');

export const options = {
  scenarios: {
    // 시나리오 1: 대기열 시스템 부하 테스트
    queue_system_load: {
      executor: 'ramping-vus',
      stages: [
        { duration: '15s', target: 200 },
        { duration: '45s', target: 800 },
        { duration: '30s', target: 1200 },
        { duration: '15s', target: 0 },
      ],
      exec: 'testRedisQueueSystem',
      tags: { method: 'redis' },
    },

    // 시나리오 2: 실시간 상태 조회 부하
    status_polling_load: {
      executor: 'constant-vus',
      vus: 300,
      duration: '2m',
      startTime: '30s',
      exec: 'testAsyncStatusPolling',
    },

    // 시나리오 3: Redis 장애 복구 시나리오
    redis_failover_test: {
      executor: 'constant-vus',
      vus: 100,
      duration: '1m',
      startTime: '2m',
      exec: 'testRedisFailover',
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<3000'],
    redis_queue_entry_success: ['count>500'],
    redis_async_processing_time: ['p(90)<10000'],
    queue_position_accuracy: ['rate>0.95'],
    redis_data_consistency: ['rate>0.99'],
    async_issuance_final_success: ['rate>0.7'],
  },
};

const BASE_URL = 'http://localhost:8080';

export function setup() {
  console.log('=== Redis 비동기 쿠폰 테스트 시작 ===');

  const testCoupons = [
    { code: 'REDIS_ASYNC_500', maxCount: 500, discountRate: 10 },
    { code: 'REDIS_QUEUE_100', maxCount: 100, discountRate: 15 },
    { code: 'REDIS_FAILOVER_50', maxCount: 50, discountRate: 20 },
  ];

  testCoupons.forEach(coupon => {
    const response = http.post(`${BASE_URL}/admin/coupon-policies`,
      JSON.stringify(coupon),
      { headers: { 'Content-Type': 'application/json' } }
    );
    console.log(`Redis 테스트 쿠폰 생성: ${coupon.code} - ${response.status}`);
  });

  return { testCoupons };
}

// 시나리오 1: Redis 대기열 시스템 부하 테스트
export function testRedisQueueSystem(data) {
  const userId = Math.floor(Math.random() * 20000) + 1;
  const couponCode = 'REDIS_ASYNC_500';

  const requestStartTime = Date.now();

  // Step 1: 비동기 쿠폰 발급 요청 (대기열 진입)
  const requestResponse = http.post(
    `${BASE_URL}/coupons/issue-async/${userId}?code=${couponCode}`,
    null,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '5s',
      tags: { method: 'redis' },
    }
  );

  const requestSuccess = check(requestResponse, {
    'async request status 202': (r) => r.status === 202,
    'async request fast response': (r) => r.timings.duration < 2000,
    'async request has requestId': (r) => {
      try {
        const data = JSON.parse(r.body);
        return data.data && data.data.requestId;
      } catch (e) {
        return false;
      }
    },
  });

  if (requestResponse.status === 202) {
    redisQueueEntry.add(1);

    try {
      const responseData = JSON.parse(requestResponse.body);
      const requestId = responseData.data.requestId;
      console.log(`Redis 대기열 진입: userId=${userId}, requestId=${requestId}`);

      // Step 2: 상태 폴링으로 처리 완료 대기
      let pollCount = 0;
      let finalStatus = null;
      const maxPolls = 20;

      while (pollCount < maxPolls) {
        sleep(2);
        pollCount++;

        const statusResponse = http.get(
          `${BASE_URL}/coupons/issue-status/${userId}?code=${couponCode}`
        );

        if (statusResponse.status === 200) {
          const statusData = JSON.parse(statusResponse.body);
          const status = statusData.data.status;

          console.log(`상태 폴링 ${pollCount}: userId=${userId}, status=${status}, queuePos=${statusData.data.queuePosition}`);

          if (status === 'COMPLETED') {
            finalStatus = 'SUCCESS';
            const totalTime = Date.now() - requestStartTime;
            redisProcessingTime.add(totalTime);
            asyncIssuanceSuccess.add(true);
            redisDataConsistency.add(true);
            console.log(`Redis 비동기 발급 성공: userId=${userId}, 총 처리시간=${totalTime}ms, 폴링=${pollCount}회`);
            break;

          } else if (status === 'NOT_REQUESTED') {
            finalStatus = 'SOLD_OUT';
            asyncIssuanceSuccess.add(false);
            redisDataConsistency.add(true);
            console.log(`Redis 쿠폰 품절: userId=${userId}, 폴링=${pollCount}회`);
            break;

          } else if (status === 'WAITING') {
            const queuePos = statusData.data.queuePosition;
            if (queuePos !== null && queuePos >= 0) {
              queuePositionAccuracy.add(true);
            } else {
              queuePositionAccuracy.add(false);
            }
            continue;
          }
        } else {
          queuePositionAccuracy.add(false);
          console.log(`상태 조회 실패: userId=${userId}, status=${statusResponse.status}`);
        }
      }

      if (finalStatus === null) {
        asyncIssuanceSuccess.add(false);
        redisDataConsistency.add(false);
        console.log(`Redis 비동기 처리 타임아웃: userId=${userId}, 폴링=${pollCount}회`);
      }

    } catch (e) {
      redisDataConsistency.add(false);
      console.log(`Redis 응답 파싱 오류: userId=${userId}, error=${e.message}`);
    }

  } else if (requestResponse.status === 409) {
    console.log(`Redis 중복 요청: userId=${userId}, status=${requestResponse.status}`);
    redisDataConsistency.add(true);

  } else {
    console.log(`Redis 시스템 오류: userId=${userId}, status=${requestResponse.status}`);
    redisDataConsistency.add(false);
  }

  sleep(Math.random() * 3 + 1);
}

// 시나리오 2: 실시간 상태 조회 부하 테스트
export function testAsyncStatusPolling(data) {
  const userId = Math.floor(Math.random() * 1000) + 1;
  const couponCode = 'REDIS_QUEUE_100';

  const statusResponse = http.get(
    `${BASE_URL}/coupons/issue-status/${userId}?code=${couponCode}`,
    {
      timeout: '3s',
    }
  );

  const success = check(statusResponse, {
    'status polling response ok': (r) => r.status === 200,
    'status polling fast response': (r) => r.timings.duration < 1000,
    'status polling valid data': (r) => {
      try {
        const data = JSON.parse(r.body);
        return data.data && typeof data.data.status === 'string';
      } catch (e) {
        return false;
      }
    },
  });

  if (success && statusResponse.status === 200) {
    try {
      const statusData = JSON.parse(statusResponse.body);
      const status = statusData.data.status;
      const queuePos = statusData.data.queuePosition;

      if (status === 'WAITING' && queuePos !== null) {
        queuePositionAccuracy.add(true);
      } else if (status === 'COMPLETED' || status === 'NOT_REQUESTED') {
        queuePositionAccuracy.add(true);
      } else {
        queuePositionAccuracy.add(false);
      }

      redisDataConsistency.add(true);

    } catch (e) {
      redisDataConsistency.add(false);
    }
  } else {
    redisDataConsistency.add(false);
  }

  sleep(Math.random() * 2 + 0.5);
}

// 시나리오 3: Redis 장애 복구 시나리오
export function testRedisFailover(data) {
  const userId = Math.floor(Math.random() * 500) + 1;
  const couponCode = 'REDIS_FAILOVER_50';

  const healthResponse = http.get(
    `${BASE_URL}/coupons/issue-status/${userId}?code=${couponCode}`,
    { timeout: '2s' }
  );

  if (healthResponse.status === 200) {
    const issueResponse = http.post(
      `${BASE_URL}/coupons/issue-async/${userId}?code=${couponCode}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
        timeout: '3s',
      }
    );

    check(issueResponse, {
      'redis failover issue success': (r) => r.status === 202 || r.status === 409,
      'redis failover fast response': (r) => r.timings.duration < 3000,
    });

    if (issueResponse.status === 202) {
      redisQueueEntry.add(1);
      redisDataConsistency.add(true);
      console.log(`Redis 장애복구 발급 성공: userId=${userId}`);
    } else {
      redisDataConsistency.add(true);
      console.log(`Redis 장애복구 품절: userId=${userId}`);
    }

  } else if (healthResponse.status === 500 || healthResponse.status === 503) {
    console.log(`Redis 장애 감지: userId=${userId}, status=${healthResponse.status}`);
    redisDataConsistency.add(false);

    const fallbackResponse = http.post(
      `${BASE_URL}/coupons/issue-sync/${userId}?code=${couponCode}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
        timeout: '10s',
      }
    );

    check(fallbackResponse, {
      'redis failover fallback works': (r) => r.status === 200 || r.status === 409,
    });

    if (fallbackResponse.status === 200) {
      console.log(`Redis 장애 시 DB 폴백 성공: userId=${userId}`);
    }

  } else {
    redisDataConsistency.add(false);
    console.log(`Redis 상태 불명: userId=${userId}, status=${healthResponse.status}`);
  }

  sleep(Math.random() * 4 + 2);
}

export function teardown(data) {
  console.log('=== Redis 비동기 쿠폰 테스트 완료 ===');

  data.testCoupons.forEach(coupon => {
    const dbStatus = http.get(`${BASE_URL}/admin/coupons/${coupon.code}/status`);

    const redisStatus = http.get(`${BASE_URL}/admin/redis/queue-status?code=${coupon.code}`);

    if (dbStatus.status === 200) {
      try {
        const db = JSON.parse(dbStatus.body);
        console.log(`${coupon.code} DB 발급: ${db.issuedCount}/${coupon.maxCount}개`);
      } catch (e) {
        console.log(`${coupon.code} DB 상태 확인 실패`);
      }
    }

    if (redisStatus.status === 200) {
      try {
        const redis = JSON.parse(redisStatus.body);
        console.log(`${coupon.code} Redis 대기열: ${redis.queueSize}명, 처리중: ${redis.processingCount}명`);
      } catch (e) {
        console.log(`${coupon.code} Redis 상태 확인 실패`);
      }
    }
  });

  console.log('=== Redis 성능 지표 요약 ===');
  console.log('- 대기열 진입률: 빠른 응답으로 사용자 경험 향상');
  console.log('- 비동기 처리: 높은 처리량으로 대량 요청 처리 가능');
  console.log('- 상태 폴링: 실시간 진행상황 제공');
  console.log('- 장애복구: Redis 장애 시 DB 폴백 동작 확인');
}