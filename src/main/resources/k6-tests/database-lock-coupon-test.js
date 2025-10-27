import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const successfulCouponIssue = new Counter('successful_coupon_issue_db_lock');
const failedCouponIssue = new Counter('failed_coupon_issue_db_lock');
const lockWaitTime = new Trend('database_lock_wait_time');
const dataConsistency = new Rate('coupon_data_consistency');

export const options = {
  scenarios: {
    // 시나리오 1: 분산락 + 낙관적 락 조합 테스트
    distributed_optimistic_lock: {
      executor: 'ramping-vus',
      stages: [
        { duration: '10s', target: 100 },  // 빠른 증가
        { duration: '30s', target: 300 },  // 높은 동시성
        { duration: '20s', target: 500 },  // 최대 부하
        { duration: '10s', target: 0 },    // 종료
      ],
      exec: 'testDistributedLock',
      env: { LOCK_TYPE: 'distributed' },
    },

    // 시나리오 2: 순수 낙관적 락 스트레스 테스트
    pure_optimistic_lock: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 1000,  // 정확히 1000명이 100개 쿠폰에 동시 접근
      startTime: '1m',
      maxDuration: '2m',
      exec: 'testOptimisticLock',
      env: { LOCK_TYPE: 'optimistic' },
    },

    // 시나리오 3: 데드락 상황 시뮬레이션
    deadlock_simulation: {
      executor: 'constant-vus',
      vus: 50,
      duration: '1m',
      startTime: '3m',
      exec: 'testDeadlockScenario',
      env: { LOCK_TYPE: 'deadlock_test' },
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<10000'],        // DB 락 특성상 더 긴 허용시간
    successful_coupon_issue_db_lock: ['count>90'], // 최소 90개 발급 성공
    coupon_data_consistency: ['rate>0.999'],   // 데이터 일관성 99.9% 이상
    database_lock_wait_time: ['p(90)<5000'],   // 90% 락 대기시간 5초 이내
  },
};

const BASE_URL = 'http://localhost:8080';

export function setup() {
  console.log('=== DB 락 기반 쿠폰 테스트 시작 ===');

  // 테스트용 쿠폰 정책 생성
  const testCoupons = [
    { code: 'DB_LOCK_TEST_100', maxCount: 100, discountRate: 10 },
    { code: 'OPTIMISTIC_TEST_50', maxCount: 50, discountRate: 15 },
    { code: 'DEADLOCK_TEST_30', maxCount: 30, discountRate: 20 },
  ];

  testCoupons.forEach(coupon => {
    const response = http.post(`${BASE_URL}/admin/coupon-policies`,
      JSON.stringify(coupon),
      { headers: { 'Content-Type': 'application/json' } }
    );
    console.log(`쿠폰 정책 생성: ${coupon.code} - ${response.status}`);
  });

  return { testCoupons };
}

// 시나리오 1: 분산락 + 낙관적 락 조합 테스트 (issueCoupon 메소드)
export function testDistributedLock(data) {
  const userId = Math.floor(Math.random() * 10000) + 1;
  const couponCode = 'DB_LOCK_TEST_100';

  const startTime = Date.now();

  // 분산락이 적용된 일반 쿠폰 발급 API 호출
  const response = http.post(
    `${BASE_URL}/coupons/issue-sync/${userId}?code=${couponCode}`,
    null,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '15s', // 분산락 대기시간 고려
    }
  );

  const responseTime = Date.now() - startTime;
  lockWaitTime.add(responseTime);

  const success = check(response, {
    'distributed lock status 200 or 409': (r) => r.status === 200 || r.status === 409,
    'distributed lock response time < 12s': (r) => r.timings.duration < 12000,
    'distributed lock valid response': (r) => r.body && r.body.length > 0,
  });

  if (response.status === 200) {
    successfulCouponIssue.add(1);
    dataConsistency.add(true);

    try {
      const result = JSON.parse(response.body);
      console.log(`분산락 쿠폰 발급 성공: userId=${userId}, couponId=${result.data?.couponId}, 대기시간=${responseTime}ms`);
    } catch (e) {
      console.log(`분산락 쿠폰 발급 성공: userId=${userId}, 대기시간=${responseTime}ms`);
    }

  } else if (response.status === 409) {
    // 품절 또는 중복 발급 - 정상적인 비즈니스 로직
    console.log(`분산락 쿠폰 품절: userId=${userId}, status=${response.status}`);
    dataConsistency.add(true);

  } else if (response.status === 429) {
    // 분산락 대기 시간 초과
    console.log(`분산락 대기 시간 초과: userId=${userId}, 응답시간=${responseTime}ms`);
    dataConsistency.add(false);

  } else {
    failedCouponIssue.add(1);
    dataConsistency.add(false);
    console.log(`분산락 시스템 오류: userId=${userId}, status=${response.status}, body=${response.body}`);
  }

  sleep(Math.random() * 2 + 1); // 1-3초 대기
}

// 시나리오 2: 순수 낙관적 락 테스트 (useCouponOptimistic 기반)
export function testOptimisticLock(data) {
  const userId = Math.floor(Math.random() * 5000) + 1;
  const couponCode = 'OPTIMISTIC_TEST_50';

  let retryCount = 0;
  const maxRetries = 3;

  while (retryCount < maxRetries) {
    const startTime = Date.now();

    // 낙관적 락이 적용된 쿠폰 발급 API 호출
    const response = http.post(
      `${BASE_URL}/coupons/issue-optimistic/${userId}?code=${couponCode}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
        timeout: '10s',
      }
    );

    const responseTime = Date.now() - startTime;

    const success = check(response, {
      'optimistic lock status 200, 409, or 423': (r) =>
        r.status === 200 || r.status === 409 || r.status === 423,
      'optimistic lock fast response': (r) => r.timings.duration < 3000,
    });

    if (response.status === 200) {
      successfulCouponIssue.add(1);
      dataConsistency.add(true);
      console.log(`낙관적 락 성공: userId=${userId}, retry=${retryCount}, 응답시간=${responseTime}ms`);
      break;

    } else if (response.status === 409) {
      // 품절
      console.log(`낙관적 락 품절: userId=${userId}, retry=${retryCount}`);
      dataConsistency.add(true);
      break;

    } else if (response.status === 423) {
      // OptimisticLockingFailureException - 재시도
      retryCount++;
      console.log(`낙관적 락 충돌: userId=${userId}, retry=${retryCount}/${maxRetries}`);

      if (retryCount >= maxRetries) {
        failedCouponIssue.add(1);
        dataConsistency.add(false);
        console.log(`낙관적 락 최대 재시도 초과: userId=${userId}`);
      } else {
        // 지수 백오프 적용
        sleep(Math.pow(2, retryCount) * 0.1); // 0.2초, 0.4초, 0.8초
        continue;
      }

    } else {
      failedCouponIssue.add(1);
      dataConsistency.add(false);
      console.log(`낙관적 락 시스템 오류: userId=${userId}, status=${response.status}`);
      break;
    }
  }

  sleep(Math.random() * 1 + 0.5); // 0.5-1.5초
}

// 시나리오 3: 데드락 상황 시뮬레이션
export function testDeadlockScenario(data) {
  const userId = Math.floor(Math.random() * 100) + 1;
  const couponCodes = ['DEADLOCK_TEST_30', 'DB_LOCK_TEST_100'];

  // 동시에 여러 쿠폰 발급 시도 (데드락 유발 가능성)
  const requests = couponCodes.map(code => {
    return {
      method: 'POST',
      url: `${BASE_URL}/coupons/issue-sync/${userId}?code=${code}`,
      params: {
        headers: { 'Content-Type': 'application/json' },
        timeout: '20s',
      }
    };
  });

  const startTime = Date.now();

  // 병렬 요청 실행
  const responses = http.batch(requests);

  const totalTime = Date.now() - startTime;
  lockWaitTime.add(totalTime);

  let successCount = 0;
  let deadlockDetected = false;

  responses.forEach((response, index) => {
    const code = couponCodes[index];

    check(response, {
      [`deadlock test ${code} valid status`]: (r) =>
        r.status === 200 || r.status === 409 || r.status === 408 || r.status === 500,
    });

    if (response.status === 200) {
      successCount++;
      successfulCouponIssue.add(1);

    } else if (response.status === 408 || response.status === 500) {
      // 타임아웃 또는 데드락 감지
      deadlockDetected = true;
      console.log(`데드락 감지 가능: userId=${userId}, code=${code}, status=${response.status}, 시간=${totalTime}ms`);

    } else if (response.status === 409) {
      console.log(`데드락 테스트 품절: userId=${userId}, code=${code}`);
    }
  });

  // 데이터 일관성 검증
  if (deadlockDetected) {
    dataConsistency.add(false);
  } else {
    dataConsistency.add(true);
  }

  console.log(`데드락 테스트 완료: userId=${userId}, 성공=${successCount}/2, 총시간=${totalTime}ms`);

  sleep(Math.random() * 3 + 2); // 2-5초 대기
}

export function teardown(data) {
  console.log('=== DB 락 기반 쿠폰 테스트 완료 ===');

  // 최종 발급 현황 확인
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

  console.log('=== 락 메커니즘 성능 분석 ===');
  console.log('- 분산락: 높은 일관성, 긴 대기시간');
  console.log('- 낙관적락: 빠른 응답, 충돌 시 재시도 필요');
  console.log('- 데드락 검출: 시스템 안정성 확인');
}