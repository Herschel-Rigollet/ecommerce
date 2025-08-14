package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Testcontainers
@SqlGroup(@Sql(scripts = {"/sql/cleanup-test-data.sql", "/sql/coupon-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD))
@Slf4j
public class CouponDistributedLockIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("선착순 쿠폰 분산락 - 1000명이 동시에 100개 한정 쿠폰 발급")
    void 분산락_선착순쿠폰_1000명_동시발급_100개한정() throws Exception {
        // Given
        String couponCode = "WELCOME100";
        int requestCount = 1000;
        int expectedIssuedCount = 100;

        // 쿠폰 정책 확인
        CouponPolicy policy = couponPolicyRepository.findByCode(couponCode).orElseThrow();
        assertThat(policy.getMaxCount()).isEqualTo(100);

        log.info("테스트 시작 - 쿠폰코드: {}, 최대발급수: {}, 총요청: {}",
                couponCode, policy.getMaxCount(), requestCount);

        // When
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        List<Long> issuedCouponIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < requestCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Coupon coupon = couponService.issueCoupon(userId, couponCode);
                    successCount.incrementAndGet();
                    issuedCouponIds.add(coupon.getCouponId());

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("User-" + userId + ": " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(expectedIssuedCount);
        assertThat(failCount.get()).isEqualTo(requestCount - expectedIssuedCount);

        // DB에서 실제 발급된 쿠폰 수 확인
        long actualIssuedCount = couponRepository.countByCode(couponCode);
        assertThat(actualIssuedCount).isEqualTo(expectedIssuedCount);

        // 발급된 쿠폰 ID들이 중복되지 않는지 확인
        Set<Long> uniqueCouponIds = new HashSet<>(issuedCouponIds);
        assertThat(uniqueCouponIds.size()).isEqualTo(successCount.get());

        // 모든 실패는 "쿠폰이 모두 소진되었습니다" 메시지여야 함
        long exhaustedErrors = errors.stream()
                .filter(error -> error.contains("쿠폰이 모두 소진되었습니다"))
                .count();
        assertThat(exhaustedErrors).isEqualTo(failCount.get());

        System.out.println("=== 선착순 쿠폰 분산락 1000명 동시 발급 테스트 결과 ===");
        System.out.println("총 요청: " + requestCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("실제 발급된 쿠폰 수: " + actualIssuedCount);
        System.out.println("테스트 시간: " + (testEndTime - testStartTime) + "ms");
        System.out.println("처리량 (TPS): " + String.format("%.2f",
                (double) successCount.get() / ((testEndTime - testStartTime) / 1000.0)));

        executor.shutdown();
    }

    @Test
    @DisplayName("선착순 쿠폰 분산락 - 초한정 쿠폰 5개 한정 동시 발급")
    void 분산락_초한정쿠폰_5개한정_동시발급() throws Exception {
        // Given
        String couponCode = "FLASH5";
        int requestCount = 100;
        int expectedIssuedCount = 5;

        // When
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> successfulUserIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < requestCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Coupon coupon = couponService.issueCoupon(userId, couponCode);
                    successCount.incrementAndGet();
                    successfulUserIds.add(userId);

                    log.info("쿠폰 발급 성공: userId={}, couponId={}", userId, coupon.getCouponId());

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("쿠폰 발급 실패: userId={}, 에러={}", userId, e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(expectedIssuedCount);
        assertThat(failCount.get()).isEqualTo(requestCount - expectedIssuedCount);

        // 실제 발급된 쿠폰 검증
        long actualIssuedCount = couponRepository.countByCode(couponCode);
        assertThat(actualIssuedCount).isEqualTo(expectedIssuedCount);

        // 중복 발급 없음 검증
        Set<Long> uniqueUserIds = new HashSet<>(successfulUserIds);
        assertThat(uniqueUserIds.size()).isEqualTo(successCount.get());

        System.out.println("=== 초한정 쿠폰(5개) 분산락 테스트 결과 ===");
        System.out.println("총 요청: " + requestCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("성공한 사용자 ID: " + successfulUserIds);
        System.out.println("테스트 시간: " + (testEndTime - testStartTime) + "ms");

        executor.shutdown();
    }

    @Test
    @DisplayName("선착순 쿠폰 분산락 - 대량 쿠폰 1000개 동시 발급 성능 테스트")
    void 분산락_대량쿠폰_1000개_성능테스트() throws Exception {
        // Given
        String couponCode = "MEGA1000";
        int requestCount = 1000;
        int expectedIssuedCount = 1000;

        // When
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    couponService.issueCoupon(userId, couponCode);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.debug("쿠폰 발급 실패: userId={}, 에러={}", userId, e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(expectedIssuedCount);

        long totalTime = testEndTime - testStartTime;
        double tps = (double) successCount.get() / (totalTime / 1000.0);

        System.out.println("=== 대량 쿠폰(1000개) 분산락 성능 테스트 결과 ===");
        System.out.println("성공한 발급: " + successCount.get());
        System.out.println("총 처리 시간: " + totalTime + "ms");
        System.out.println("처리량 (TPS): " + String.format("%.2f", tps));

        // 최소 성능 기준 검증
        assertThat(tps).isGreaterThan(10.0); // 최소 10 TPS 이상

        executor.shutdown();
    }

    @Test
    @DisplayName("선착순 쿠폰 분산락 - 여러 쿠폰 동시 발급 (락 키 격리 검증)")
    void 분산락_여러쿠폰_동시발급_락키격리() throws Exception {
        // Given - 3개의 서로 다른 쿠폰
        String[] couponCodes = {"WELCOME100", "SPECIAL50", "LIMITED10"};
        int[] expectedCounts = {100, 50, 10};
        int requestPerCoupon = 200;

        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(couponCodes.length * requestPerCoupon);

        Map<String, AtomicInteger> successCounts = new HashMap<>();
        Map<String, AtomicInteger> failCounts = new HashMap<>();

        for (String code : couponCodes) {
            successCounts.put(code, new AtomicInteger(0));
            failCounts.put(code, new AtomicInteger(0));
        }

        // When - 3개 쿠폰을 동시에 발급 요청
        for (int couponIndex = 0; couponIndex < couponCodes.length; couponIndex++) {
            final String couponCode = couponCodes[couponIndex];
            final int startUserId = couponIndex * requestPerCoupon + 1;

            for (int i = 0; i < requestPerCoupon; i++) {
                final long userId = startUserId + i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        couponService.issueCoupon(userId, couponCode);
                        successCounts.get(couponCode).incrementAndGet();

                    } catch (Exception e) {
                        failCounts.get(couponCode).incrementAndGet();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
        }

        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        // Then
        assertThat(completed).isTrue();

        System.out.println("=== 여러 쿠폰 동시 발급 락 키 격리 테스트 결과 ===");

        for (int i = 0; i < couponCodes.length; i++) {
            String couponCode = couponCodes[i];
            int expectedCount = expectedCounts[i];
            int actualSuccess = successCounts.get(couponCode).get();
            int actualFail = failCounts.get(couponCode).get();

            // 각 쿠폰별 정확한 발급 수 검증
            assertThat(actualSuccess).isEqualTo(expectedCount);
            assertThat(actualFail).isEqualTo(requestPerCoupon - expectedCount);

            // DB에서 실제 발급 수 재확인
            long dbCount = couponRepository.countByCode(couponCode);
            assertThat(dbCount).isEqualTo(expectedCount);

            System.out.println(String.format("쿠폰 %s: 성공=%d, 실패=%d, DB확인=%d",
                    couponCode, actualSuccess, actualFail, dbCount));
        }

        System.out.println("테스트 시간: " + (testEndTime - testStartTime) + "ms");

        executor.shutdown();
    }

    @Test
    @DisplayName("선착순 쿠폰 분산락 - 중복 발급 방지 검증")
    void 분산락_중복발급방지_검증() throws Exception {
        // Given - 같은 사용자가 여러 번 요청하는 시나리오는 비즈니스 로직에 따라 다름
        // 여기서는 서로 다른 사용자가 발급받는 시나리오로 중복 검증
        String couponCode = "LIMITED10";
        int requestCount = 50;
        int expectedIssuedCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(25);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        Set<Long> issuedCouponIds = Collections.synchronizedSet(new HashSet<>());
        Set<Long> successfulUserIds = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger successCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < requestCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Coupon coupon = couponService.issueCoupon(userId, couponCode);
                    successCount.incrementAndGet();
                    issuedCouponIds.add(coupon.getCouponId());
                    successfulUserIds.add(userId);

                } catch (Exception e) {
                    // 실패는 정상 (10개 초과)
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(expectedIssuedCount);

        // 중복 발급 검증
        assertThat(issuedCouponIds.size()).isEqualTo(successCount.get()); // 쿠폰 ID 중복 없음
        assertThat(successfulUserIds.size()).isEqualTo(successCount.get()); // 사용자 ID 중복 없음

        System.out.println("=== 중복 발급 방지 검증 테스트 결과 ===");
        System.out.println("발급된 쿠폰 ID 수: " + issuedCouponIds.size());
        System.out.println("성공한 사용자 수: " + successfulUserIds.size());
        System.out.println("총 성공 횟수: " + successCount.get());

        executor.shutdown();
    }

    @Test
    @DisplayName("선착순 쿠폰 분산락 - 락과 트랜잭션 순서 검증")
    void 분산락_락과트랜잭션순서_검증() {
        // Given
        String couponCode = "SPECIAL50";
        Long userId = 1L;

        // When & Then - 단일 요청으로 락과 트랜잭션 순서 검증
        assertDoesNotThrow(() -> {
            Coupon result = couponService.issueCoupon(userId, couponCode);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getCode()).isEqualTo(couponCode);
            assertThat(result.getDiscountRate()).isEqualTo(20); // SPECIAL50은 20% 할인
            assertThat(result.isUsed()).isFalse();
            assertThat(result.getCouponId()).isNotNull();

            log.info("락과 트랜잭션 순서 검증 완료: couponId={}, userId={}, code={}",
                    result.getCouponId(), result.getUserId(), result.getCode());
        });
    }

    @Test
    @DisplayName("선착순 쿠폰 분산락 - 존재하지 않는 쿠폰 코드 예외 처리")
    void 분산락_존재하지않는쿠폰코드_예외처리() {
        // Given
        String invalidCouponCode = "INVALID_CODE";
        Long userId = 1L;

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, invalidCouponCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 쿠폰 코드입니다");
    }
}