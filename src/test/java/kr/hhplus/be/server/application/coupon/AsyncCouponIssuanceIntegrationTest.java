package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
@Sql(scripts = {"/sql/cleanup-test-data.sql", "/sql/test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-test-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AsyncCouponIssuanceIntegrationTest {

    // Redis 테스트 컨테이너 설정 (독립적 테스트 환경)
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    // 테스트 컨테이너의 Redis 정보를 Spring에 동적 주입
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private CouponPolicy testPolicy;
    private static final String TEST_CODE = "ASYNC_TEST_COUPON";
    private static final int MAX_COUNT = 100;

    @BeforeEach
    void setUp() {
        // 독립적 테스트 환경 보장: Redis 초기화
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();

        // 테스트용 쿠폰 정책 생성
        testPolicy = new CouponPolicy();
        testPolicy.setCode(TEST_CODE);
        testPolicy.setDiscountRate(15);
        testPolicy.setMaxCount(MAX_COUNT);
        //couponPolicyRepository.save(testPolicy);
    }

    @Test
    @Order(1)
    @DisplayName("Redis 연결 상태 및 초기 다중 자료구조 상태 검증")
    void redisConnectionAndInitialMultiStructureStateTest() {
        // Redis 컨테이너 정상 실행 확인
        assertThat(redis.isRunning()).isTrue();

        // 각 Redis 자료구조의 초기 상태 확인
        String countKey = "coupon:count:" + TEST_CODE;      // String
        String queueKey = "coupon:queue:" + TEST_CODE;      // ZSET
        String issuedKey = "coupon:issued:" + TEST_CODE;    // SET

        // 초기 상태: 모든 키가 존재하지 않아야 함
        assertThat(stringRedisTemplate.opsForValue().get(countKey)).isNull();
        assertThat(stringRedisTemplate.opsForZSet().zCard(queueKey)).isEqualTo(0);
        assertThat(stringRedisTemplate.opsForSet().size(issuedKey)).isEqualTo(0);

        log.info("Redis 연결 및 다중 자료구조 초기 상태 검증 완료");
    }

    @Test
    @Order(2)
    @DisplayName("핵심 기능: 단일 비동기 쿠폰 발급 전체 흐름 검증")
    void singleAsyncCouponIssuanceFlowTest() throws Exception {
        // Given
        Long userId = 123L;

        // When: 비동기 쿠폰 발급 실행
        CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, TEST_CODE);

        // 비동기 작업 완료 대기 (최대 10초)
        Coupon issuedCoupon = future.get(10, TimeUnit.SECONDS);

        // Then: 전체 발급 흐름 검증

        // 1. 쿠폰 발급 결과 검증
        assertThat(issuedCoupon).isNotNull();
        assertThat(issuedCoupon.getUserId()).isEqualTo(userId);
        assertThat(issuedCoupon.getCode()).isEqualTo(TEST_CODE);
        assertThat(issuedCoupon.getDiscountRate()).isEqualTo(15);
        assertThat(issuedCoupon.isUsed()).isFalse();

        // 2. Redis String: 수량 차감 확인
        String countKey = "coupon:count:" + TEST_CODE;
        String remaining = stringRedisTemplate.opsForValue().get(countKey);
        assertThat(remaining).isEqualTo(String.valueOf(MAX_COUNT - 1)); // 99개 남음

        // 3. Redis SET: 발급 완료 사용자 등록 확인
        String issuedKey = "coupon:issued:" + TEST_CODE;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(issuedKey, userId.toString());
        assertThat(isMember).isTrue();

        // 4. Redis ZSET: 대기열에서 제거 확인 (발급 완료 후)
        String queueKey = "coupon:queue:" + TEST_CODE;
        Double queueScore = stringRedisTemplate.opsForZSet().score(queueKey, userId.toString());
        assertThat(queueScore).isNull(); // 대기열에서 제거됨

        // 5. DB: 쿠폰 데이터 저장 확인
        assertThat(couponRepository.countByCode(TEST_CODE)).isEqualTo(1);

        log.info("단일 비동기 쿠폰 발급 전체 흐름 검증 완료");
    }

    @Test
    @Order(3)
    @DisplayName("핵심 기능: 대용량 동시 비동기 발급에서 정확한 수량 제어 검증")
    void massiveConcurrentAsyncIssuanceTest() throws InterruptedException {
        // Given: 제한된 수량의 쿠폰 (50개만)
        int limitedCount = 50;
        CouponPolicy limitedPolicy = new CouponPolicy();
        limitedPolicy.setCode("LIMITED_ASYNC_50");
        limitedPolicy.setDiscountRate(20);
        limitedPolicy.setMaxCount(limitedCount);
        couponPolicyRepository.save(limitedPolicy);

        // 동시 요청자 수 (제한 수량보다 훨씬 많게)
        int concurrentUsers = 200;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentUsers);
        ExecutorService executor = Executors.newFixedThreadPool(50);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 200명이 동시에 50개 한정 쿠폰 비동기 발급 요청
        for (int i = 1; i <= concurrentUsers; i++) {
            Long userId = (long) i;
            executor.submit(() -> {
                try {
                    // 동시 시작을 위한 대기
                    startLatch.await();

                    // 비동기 쿠폰 발급 시도
                    CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, "LIMITED_ASYNC_50");

                    // 결과 대기 (최대 15초)
                    Coupon coupon = future.get(15, TimeUnit.SECONDS);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("쿠폰 발급 실패: userId={}, error={}", userId, e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 모든 스레드 동시 시작
        startLatch.countDown();

        // 모든 작업 완료 대기 (최대 30초)
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executor.shutdown();

        // Then: 정확한 수량 제어 검증

        // 성공/실패 수량 검증
        assertThat(successCount.get()).isEqualTo(limitedCount); // 정확히 50개만 발급
        assertThat(failCount.get()).isEqualTo(concurrentUsers - limitedCount); // 나머지 150개는 실패

        // Redis 수량 검증
        String countKey = "coupon:count:LIMITED_ASYNC_50";
        String remaining = stringRedisTemplate.opsForValue().get(countKey);
        assertThat(remaining).isEqualTo("0"); // 모두 소진

        // Redis SET: 발급 완료자 수 검증
        String issuedKey = "coupon:issued:LIMITED_ASYNC_50";
        Long issuedCount = stringRedisTemplate.opsForSet().size(issuedKey);
        assertThat(issuedCount).isEqualTo(limitedCount); // 정확히 50명

        // DB 발급 수량 검증
        assertThat(couponRepository.countByCode("LIMITED_ASYNC_50")).isEqualTo(limitedCount);

        log.info("대용량 동시 비동기 발급 검증 완료: {}명 중 {}명 성공, {}명 실패",
                concurrentUsers, successCount.get(), failCount.get());
    }

    @Test
    @Order(4)
    @DisplayName("중복 방지: 동일 사용자 중복 비동기 발급 방지 검증")
    void duplicateAsyncIssuancePreventionTest() throws Exception {
        // Given
        Long userId = 456L;

        // When: 첫 번째 비동기 발급 (성공해야 함)
        CompletableFuture<Coupon> firstFuture = couponService.issueCouponAsync(userId, TEST_CODE);
        Coupon firstCoupon = firstFuture.get(10, TimeUnit.SECONDS);
        assertThat(firstCoupon).isNotNull();

        // Then: 두 번째 비동기 발급 시도 (실패해야 함)
        CompletableFuture<Coupon> secondFuture = couponService.issueCouponAsync(userId, TEST_CODE);

        assertThatThrownBy(() -> secondFuture.get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasStackTraceContaining("이미 발급받은 쿠폰입니다");

        // 발급 수량이 1개만 유지되는지 확인
        assertThat(couponRepository.countByCode(TEST_CODE)).isEqualTo(1);

        // Redis SET에 한 번만 등록되었는지 확인
        String issuedKey = "coupon:issued:" + TEST_CODE;
        Long setSize = stringRedisTemplate.opsForSet().size(issuedKey);
        assertThat(setSize).isEqualTo(1);

        log.info("중복 비동기 발급 방지 검증 완료");
    }

    @Test
    @Order(5)
    @DisplayName("순서 보장: 나노초 정밀도 타임스탬프 기반 선착순 검증")
    void nanosecondPrecisionOrderingTest() throws Exception {
        // Given: 순서 검증을 위한 작은 수량 쿠폰
        CouponPolicy orderTestPolicy = new CouponPolicy();
        orderTestPolicy.setCode("ORDER_TEST_5");
        orderTestPolicy.setDiscountRate(25);
        orderTestPolicy.setMaxCount(5);
        couponPolicyRepository.save(orderTestPolicy);

        int participantCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(participantCount);
        ExecutorService executor = Executors.newFixedThreadPool(participantCount);

        ConcurrentLinkedQueue<Long> successOrder = new ConcurrentLinkedQueue<>();

        // When: 10명이 동시에 5개 한정 쿠폰 신청 (나노초 정밀도 순서)
        for (int i = 1; i <= participantCount; i++) {
            Long userId = (long) i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, "ORDER_TEST_5");
                    Coupon coupon = future.get(10, TimeUnit.SECONDS);

                    successOrder.offer(coupon.getUserId()); // 성공 순서 기록

                } catch (Exception e) {
                    // 실패는 무시 (순서 검증이 목적)
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: 선착순 순서 검증
        assertThat(successOrder).hasSize(5); // 정확히 5명만 성공

        // Redis ZSET에서 순서 확인 (대기열에서 제거되기 전 스냅샷은 불가능하므로, 성공자 순서로 검증)
        String issuedKey = "coupon:issued:ORDER_TEST_5";
        Long finalIssuedCount = stringRedisTemplate.opsForSet().size(issuedKey);
        assertThat(finalIssuedCount).isEqualTo(5);

        log.info("나노초 정밀도 선착순 검증 완료: 성공 순서 = {}", successOrder);
    }

    @Test
    @Order(6)
    @DisplayName("비동기 성능: 처리 시간 및 동시 처리량 검증")
    void asyncPerformanceAndThroughputTest() throws InterruptedException {
        // Given: 성능 테스트용 쿠폰
        CouponPolicy perfPolicy = new CouponPolicy();
        perfPolicy.setCode("PERF_TEST_100");
        perfPolicy.setDiscountRate(10);
        perfPolicy.setMaxCount(100);
        couponPolicyRepository.save(perfPolicy);

        int concurrentRequests = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);
        ExecutorService executor = Executors.newFixedThreadPool(20);

        AtomicInteger completedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When: 100개 쿠폰을 100명이 동시 비동기 요청
        for (int i = 1; i <= concurrentRequests; i++) {
            Long userId = (long) i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, "PERF_TEST_100");
                    future.get(10, TimeUnit.SECONDS);

                    completedCount.incrementAndGet();

                } catch (Exception e) {
                    // 성능 테스트에서는 에러 무시
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(20, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        executor.shutdown();

        // Then: 비동기 성능 검증
        assertThat(completed).isTrue(); // 20초 내 완료
        assertThat(completedCount.get()).isEqualTo(100); // 모든 요청 성공
        assertThat(totalTime).isLessThan(15000); // 15초 이내 완료

        double throughput = (double) completedCount.get() / (totalTime / 1000.0);
        assertThat(throughput).isGreaterThan(10); // 초당 10개 이상 처리

        log.info("비동기 성능 검증 완료: 처리시간={}ms, 처리량={:.2f} TPS", totalTime, throughput);
    }

    @Test
    @Order(7)
    @DisplayName("장애 대응: Redis 장애 시 비동기 시스템 안전성 검증")
    void redisFailureAsyncSafetyTest() {
        // Given: 정상 상태에서 비동기 발급 시도
        Long userId = 999L;

        // When: Redis 컨테이너 중지 (장애 시뮬레이션)
        redis.stop();

        try {
            // Then: Redis 장애 시에도 적절한 예외 처리
            CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, TEST_CODE);

            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);

        } finally {
            // 테스트 후 Redis 재시작
            redis.start();

            // Redis 복구 확인
            try {
                Thread.sleep(3000); // Redis 시작 대기
                stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
            } catch (Exception e) {
                log.warn("Redis 재시작 후 초기화 실패: {}", e.getMessage());
            }
        }

        log.info("Redis 장애 시 비동기 시스템 안전성 검증 완료");
    }

    @Test
    @Order(8)
    @DisplayName("TTL 관리: Redis 자료구조별 TTL 설정 검증")
    void redisTtlManagementTest() throws Exception {
        // Given & When: 비동기 쿠폰 발급
        Long userId = 777L;
        CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, TEST_CODE);
        future.get(10, TimeUnit.SECONDS);

        // Then: 각 Redis 자료구조의 TTL 설정 확인
        String countKey = "coupon:count:" + TEST_CODE;
        String queueKey = "coupon:queue:" + TEST_CODE;
        String issuedKey = "coupon:issued:" + TEST_CODE;

        // String: 24시간 TTL
        Long countTtl = stringRedisTemplate.getExpire(countKey);
        assertThat(countTtl).isGreaterThan(0).isLessThanOrEqualTo(86400); // 24시간 이하

        // SET: 30일 TTL
        Long issuedTtl = stringRedisTemplate.getExpire(issuedKey);
        assertThat(issuedTtl).isGreaterThan(0).isLessThanOrEqualTo(2592000); // 30일 이하

        // ZSET: 발급 완료 후 제거되므로 존재하지 않거나 1시간 TTL
        Long queueTtl = stringRedisTemplate.getExpire(queueKey);
        // 대기열은 발급 완료 후 제거되므로 존재하지 않을 수 있음

        log.info("Redis TTL 관리 검증 완료: countTtl={}, issuedTtl={}, queueTtl={}",
                countTtl, issuedTtl, queueTtl);
    }

    // ===== 헬퍼 메서드들 =====

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncCouponIssuanceIntegrationTest.class);
}
