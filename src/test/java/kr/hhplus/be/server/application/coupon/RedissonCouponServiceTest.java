package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RedissonCouponServiceTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 쿠폰 정책 생성
        CouponPolicy policy = new CouponPolicy();
        policy.setCode("REDISSON_TEST");
        policy.setDiscountRate(10);
        policy.setMaxCount(100); // 100개 한정
        couponPolicyRepository.save(policy);

        // 기존 쿠폰 데이터 정리
        // couponRepository.deleteByCode("REDISSON_TEST");
    }

    @Test
    @DisplayName("Redisson 분산락으로 선착순 쿠폰 1000명 동시 발급 - 정확히 100개만 발급")
    void redisson_분산락_선착순_쿠폰_동시성_테스트() throws Exception {
        // Given
        String couponCode = "REDISSON_TEST";
        int requestCount = 1000;
        int expectedIssuedCount = 100;

        // When
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        // 1000명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < requestCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 준비될 때까지 대기

                    couponService.issueCoupon(userId, couponCode);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("User-" + userId + ": " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 모든 스레드 동시 시작
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();

        // 모든 요청 완료 대기 (최대 60초)
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(expectedIssuedCount);
        assertThat(failCount.get()).isEqualTo(requestCount - expectedIssuedCount);

        // DB에서 실제 발급된 쿠폰 수 확인
        long actualIssuedCount = couponRepository.countByCode(couponCode);
        assertThat(actualIssuedCount).isEqualTo(expectedIssuedCount);

        // 모든 실패는 "쿠폰이 모두 소진되었습니다" 메시지여야 함
        long exhaustedErrors = errors.stream()
                .filter(error -> error.contains("쿠폰이 모두 소진되었습니다"))
                .count();
        assertThat(exhaustedErrors).isEqualTo(failCount.get());

        System.out.println("=== Redisson 분산락 테스트 결과 ===");
        System.out.println("총 요청: " + requestCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("실제 발급된 쿠폰 수: " + actualIssuedCount);
        System.out.println("총 테스트 시간: " + (testEndTime - testStartTime) + "ms");

        executor.shutdown();
    }

    @Test
    @DisplayName("Redisson 분산락 성능 테스트")
    void redisson_분산락_성능_테스트() throws Exception {
        // Given
        String couponCode = "REDISSON_TEST";
        int requestCount = 100;

        // When
        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < requestCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    couponService.issueCoupon(userId, couponCode);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패는 정상 (100개 초과)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;
        double tps = (double) successCount.get() / (totalTime / 1000.0);

        // Then
        System.out.println("=== Redisson 분산락 성능 결과 ===");
        System.out.println("총 처리 시간: " + totalTime + "ms");
        System.out.println("성공한 발급: " + successCount.get());
        System.out.println("처리량 (TPS): " + String.format("%.2f", tps));

        // 최소 성능 기준 확인
        assertThat(tps).isGreaterThan(5.0); // 최소 5 TPS 이상

        executor.shutdown();
    }

    @Test
    @DisplayName("락과 트랜잭션 순서 검증")
    void 락과_트랜잭션_순서_검증() {
        // Given
        String couponCode = "REDISSON_TEST";
        Long userId = 1L;

        // When & Then
        assertDoesNotThrow(() -> {
            Coupon result = couponService.issueCoupon(userId, couponCode);
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getCode()).isEqualTo(couponCode);
        });
    }
}
