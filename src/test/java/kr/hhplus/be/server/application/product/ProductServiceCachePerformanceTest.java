package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.repository.OrderItemRepository;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.presentation.dto.response.PopularProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductServiceCachePerformanceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private List<Product> testProducts;
    private List<OrderItem> testOrderItems;

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화
        clearAllCaches();

        // 테스트 데이터 준비
        setupTestData();
    }

    @Test
    @DisplayName("캐시 미적용 vs 캐시 적용 성능 비교 - 단일 요청")
    void comparePerformanceWithoutAndWithCache() {
        // Given: 초기 상태에서는 캐시가 비어있음

        // When & Then: 첫 번째 요청 (캐시 미스)
        long startTime = System.currentTimeMillis();
        List<PopularProductResponse> firstResult = productService.getTop5PopularProducts();
        long firstCallTime = System.currentTimeMillis() - startTime;

        // When & Then: 두 번째 요청 (캐시 히트)
        startTime = System.currentTimeMillis();
        List<PopularProductResponse> secondResult = productService.getTop5PopularProducts();
        long secondCallTime = System.currentTimeMillis() - startTime;

        // When & Then: 세 번째 요청 (캐시 히트)
        startTime = System.currentTimeMillis();
        List<PopularProductResponse> thirdResult = productService.getTop5PopularProducts();
        long thirdCallTime = System.currentTimeMillis() - startTime;

        // 결과 검증
        assertAll(
                () -> assertThat(firstResult).hasSize(5),
                () -> assertThat(secondResult).isEqualTo(firstResult),
                () -> assertThat(thirdResult).isEqualTo(firstResult),
                () -> assertThat(secondCallTime).isLessThan(firstCallTime),
                () -> assertThat(thirdCallTime).isLessThan(firstCallTime),
                () -> {
                    double improvement = ((double)(firstCallTime - secondCallTime) / firstCallTime) * 100;
                    System.out.printf("=== 단일 요청 성능 비교 ===\n");
                    System.out.printf("첫 번째 호출 (캐시 미스): %dms\n", firstCallTime);
                    System.out.printf("두 번째 호출 (캐시 히트): %dms\n", secondCallTime);
                    System.out.printf("세 번째 호출 (캐시 히트): %dms\n", thirdCallTime);
                    System.out.printf("성능 개선율: %.2f%%\n", improvement);
                    System.out.printf("속도 개선 배수: %.2fx\n\n", (double)firstCallTime / secondCallTime);

                    assertThat(improvement).isGreaterThan(50.0); // 최소 50% 성능 향상 기대
                }
        );
    }

    @Test
    @DisplayName("동시성 테스트 - 캐시 적용 시 동시 요청 처리 성능")
    void testConcurrentRequestsWithCache() throws InterruptedException {
        int threadCount = 100;
        int requestsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);

        // 첫 번째 요청으로 캐시 워밍업
        productService.getTop5PopularProducts();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long requestStart = System.nanoTime();
                        List<PopularProductResponse> result = productService.getTop5PopularProducts();
                        long requestTime = System.nanoTime() - requestStart;

                        totalExecutionTime.addAndGet(requestTime);

                        if (result != null && result.size() == 5) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        long totalRequests = threadCount * requestsPerThread;
        double averageResponseTime = totalExecutionTime.get() / (double) totalRequests / 1_000_000; // ms로 변환
        double tps = (double) totalRequests / (totalTime / 1000.0);

        System.out.printf("=== 캐시 적용 동시성 테스트 결과 ===\n");
        System.out.printf("스레드 수: %d, 스레드당 요청 수: %d\n", threadCount, requestsPerThread);
        System.out.printf("총 요청 수: %d\n", totalRequests);
        System.out.printf("성공한 요청 수: %d\n", successCount.get());
        System.out.printf("전체 실행 시간: %dms\n", totalTime);
        System.out.printf("평균 응답 시간: %.2fms\n", averageResponseTime);
        System.out.printf("초당 처리량 (TPS): %.2f\n\n", tps);

        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(totalRequests),
                () -> assertThat(averageResponseTime).isLessThan(10.0), // 평균 응답시간 10ms 미만
                () -> assertThat(tps).isGreaterThan(100) // TPS 100 이상
        );
    }

    @Test
    @DisplayName("캐시 없이 동시성 테스트 - 성능 비교 기준")
    void testConcurrentRequestsWithoutCache() throws InterruptedException {
        int threadCount = 10; // 캐시 없이는 더 적은 스레드로 테스트
        int requestsPerThread = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        // 매번 캐시 클리어하여 캐시 미스 상황 시뮬레이션
                        clearCache("topProducts");

                        long requestStart = System.nanoTime();
                        List<PopularProductResponse> result = productService.getTop5PopularProducts();
                        long requestTime = System.nanoTime() - requestStart;

                        totalExecutionTime.addAndGet(requestTime);

                        if (result != null && result.size() == 5) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        long totalRequests = threadCount * requestsPerThread;
        double averageResponseTime = totalExecutionTime.get() / (double) totalRequests / 1_000_000;
        double tps = (double) totalRequests / (totalTime / 1000.0);

        System.out.printf("=== 캐시 미적용 동시성 테스트 결과 ===\n");
        System.out.printf("스레드 수: %d, 스레드당 요청 수: %d\n", threadCount, requestsPerThread);
        System.out.printf("총 요청 수: %d\n", totalRequests);
        System.out.printf("성공한 요청 수: %d\n", successCount.get());
        System.out.printf("전체 실행 시간: %dms\n", totalTime);
        System.out.printf("평균 응답 시간: %.2fms\n", averageResponseTime);
        System.out.printf("초당 처리량 (TPS): %.2f\n\n", tps);

        assertThat(successCount.get()).isEqualTo(totalRequests);
    }

    @Test
    @DisplayName("캐시 동기화 테스트 - sync=true 옵션 검증")
    void testCacheSynchronization() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<List<PopularProductResponse>> results = new ArrayList<>();
        AtomicLong dbCallCount = new AtomicLong(0);

        // 동시에 여러 스레드에서 첫 번째 요청 (캐시 미스)
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    long requestStart = System.nanoTime();
                    List<PopularProductResponse> result = productService.getTop5PopularProducts();
                    long requestTime = System.nanoTime() - requestStart;

                    // DB 호출이 발생했는지 추정 (응답시간으로 판단)
                    if (requestTime > 1_000_000) { // 1ms 이상이면 DB 호출로 가정
                        dbCallCount.incrementAndGet();
                    }

                    synchronized (results) {
                        results.add(result);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;

        // 모든 결과가 동일한지 검증 (sync=true로 인해 한 번만 DB 조회되어야 함)
        List<PopularProductResponse> firstResult = results.get(0);
        for (List<PopularProductResponse> result : results) {
            assertThat(result).isEqualTo(firstResult);
        }

        System.out.printf("=== 캐시 동기화 테스트 결과 ===\n");
        System.out.printf("동시 요청 스레드 수: %d\n", threadCount);
        System.out.printf("전체 실행 시간: %dms\n", totalTime);
        System.out.printf("추정 DB 호출 횟수: %d (sync=true로 인해 1회만 호출되어야 함)\n", dbCallCount.get());
        System.out.printf("모든 스레드 동일한 결과 반환: ✓\n\n");

        // sync=true 옵션으로 인해 DB 호출이 최소화되어야 함
        assertThat(dbCallCount.get()).isLessThanOrEqualTo(3); // 허용 오차 고려
    }

    @Test
    @DisplayName("캐시 TTL 및 무효화 테스트")
    void testCacheTTLAndEviction() throws InterruptedException {
        // 첫 번째 조회 (캐시 저장)
        long startTime = System.currentTimeMillis();
        List<PopularProductResponse> firstResult = productService.getTop5PopularProducts();
        long firstCallTime = System.currentTimeMillis() - startTime;

        // 캐시 확인
        Object cachedValue = cacheManager.getCache("topProducts").get("last3days_top5");
        assertThat(cachedValue).isNotNull();

        // 캐시된 상태에서 재조회
        startTime = System.currentTimeMillis();
        List<PopularProductResponse> secondResult = productService.getTop5PopularProducts();
        long secondCallTime = System.currentTimeMillis() - startTime;

        // 캐시 수동 만료
        clearCache("topProducts");

        // 캐시가 비워졌는지 확인
        cachedValue = cacheManager.getCache("topProducts").get("last3days_top5");
        assertThat(cachedValue).isNull();

        // 다시 조회 (DB에서 다시 로드)
        startTime = System.currentTimeMillis();
        List<PopularProductResponse> thirdResult = productService.getTop5PopularProducts();
        long thirdCallTime = System.currentTimeMillis() - startTime;

        System.out.printf("=== 캐시 TTL 및 무효화 테스트 결과 ===\n");
        System.out.printf("첫 번째 호출 (캐시 미스): %dms\n", firstCallTime);
        System.out.printf("두 번째 호출 (캐시 히트): %dms\n", secondCallTime);
        System.out.printf("캐시 무효화 후 호출 (캐시 미스): %dms\n", thirdCallTime);
        System.out.printf("캐시 효과 확인: ✓\n\n");

        // 결과는 같지만 응답 시간 차이 존재
        assertAll(
                () -> assertThat(firstResult).isEqualTo(secondResult),
                () -> assertThat(secondResult).isEqualTo(thirdResult),
                () -> assertThat(secondCallTime).isLessThan(firstCallTime),
                () -> assertThat(thirdCallTime).isGreaterThan(secondCallTime)
        );
    }

    @Test
    @DisplayName("대용량 데이터 성능 테스트")
    void testLargeDataPerformance() {
        // 대용량 주문 데이터 추가 생성
        createLargeOrderData();

        // 캐시 없이 첫 번째 조회
        clearAllCaches();
        long startTime = System.currentTimeMillis();
        List<PopularProductResponse> firstResult = productService.getTop5PopularProducts();
        long firstCallTime = System.currentTimeMillis() - startTime;

        // 캐시 적용 두 번째 조회
        startTime = System.currentTimeMillis();
        List<PopularProductResponse> secondResult = productService.getTop5PopularProducts();
        long secondCallTime = System.currentTimeMillis() - startTime;

        // 세 번째 조회로 캐시 안정성 확인
        startTime = System.currentTimeMillis();
        List<PopularProductResponse> thirdResult = productService.getTop5PopularProducts();
        long thirdCallTime = System.currentTimeMillis() - startTime;

        double improvement = ((double)(firstCallTime - secondCallTime) / firstCallTime) * 100;

        System.out.printf("=== 대용량 데이터 성능 테스트 결과 ===\n");
        System.out.printf("총 주문 데이터: 약 %d건\n", testOrderItems.size() + 1000);
        System.out.printf("첫 번째 호출 (캐시 미스): %dms\n", firstCallTime);
        System.out.printf("두 번째 호출 (캐시 히트): %dms\n", secondCallTime);
        System.out.printf("세 번째 호출 (캐시 히트): %dms\n", thirdCallTime);
        System.out.printf("성능 개선율: %.2f%%\n", improvement);
        System.out.printf("속도 개선 배수: %.2fx\n\n", (double)firstCallTime / secondCallTime);

        assertAll(
                () -> assertThat(firstResult).hasSize(5),
                () -> assertThat(secondResult).isEqualTo(firstResult),
                () -> assertThat(thirdResult).isEqualTo(firstResult),
                () -> assertThat(improvement).isGreaterThan(70.0) // 대용량 데이터에서는 더 큰 성능 향상 기대
        );
    }

    @Test
    @DisplayName("반복 요청 성능 분석")
    void testRepeatedRequestsPerformance() {
        int iterations = 50;
        List<Long> cacheMissTimes = new ArrayList<>();
        List<Long> cacheHitTimes = new ArrayList<>();

        // 캐시 미스 측정 (매번 캐시 클리어)
        for (int i = 0; i < iterations; i++) {
            clearCache("topProducts");
            long startTime = System.currentTimeMillis();
            productService.getTop5PopularProducts();
            long executionTime = System.currentTimeMillis() - startTime;
            cacheMissTimes.add(executionTime);
        }

        // 캐시 히트 측정 (캐시 워밍업 후)
        clearCache("topProducts");
        productService.getTop5PopularProducts(); // 워밍업

        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            productService.getTop5PopularProducts();
            long executionTime = System.currentTimeMillis() - startTime;
            cacheHitTimes.add(executionTime);
        }

        // 통계 계산
        double avgCacheMiss = cacheMissTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgCacheHit = cacheHitTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long minCacheMiss = cacheMissTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxCacheMiss = cacheMissTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minCacheHit = cacheHitTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxCacheHit = cacheHitTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        double improvement = ((avgCacheMiss - avgCacheHit) / avgCacheMiss) * 100;

        System.out.printf("=== 반복 요청 성능 분석 (%d회) ===\n", iterations);
        System.out.printf("캐시 미스 - 평균: %.2fms, 최소: %dms, 최대: %dms\n", avgCacheMiss, minCacheMiss, maxCacheMiss);
        System.out.printf("캐시 히트 - 평균: %.2fms, 최소: %dms, 최대: %dms\n", avgCacheHit, minCacheHit, maxCacheHit);
        System.out.printf("평균 성능 향상: %.2f%%\n", improvement);
        System.out.printf("평균 속도 개선 배수: %.2fx\n\n", avgCacheMiss / avgCacheHit);

        assertThat(improvement).isGreaterThan(60.0);
    }

    @Test
    @DisplayName("메모리 사용량 테스트")
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();

        // GC 실행 및 초기 메모리 측정
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // 캐시에 데이터 저장
        for (int i = 0; i < 100; i++) {
            productService.getTop5PopularProducts();
        }

        // 메모리 사용량 측정
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = memoryAfter - memoryBefore;

        System.out.printf("=== 메모리 사용량 테스트 결과 ===\n");
        System.out.printf("캐시 사용 전 메모리: %,d bytes (%.2f MB)\n", memoryBefore, memoryBefore / 1024.0 / 1024.0);
        System.out.printf("캐시 사용 후 메모리: %,d bytes (%.2f MB)\n", memoryAfter, memoryAfter / 1024.0 / 1024.0);
        System.out.printf("캐시로 인한 메모리 증가: %,d bytes (%.2f KB)\n", memoryUsed, memoryUsed / 1024.0);
        System.out.printf("메모리 효율성: ✓\n\n");

        // 메모리 사용량이 합리적인 범위 내인지 확인 (5MB 미만)
        assertThat(Math.abs(memoryUsed)).isLessThan(5 * 1024 * 1024);
    }

    // === 유틸리티 메서드 ===

    private void setupTestData() {
        // 테스트 상품 데이터 생성
        testProducts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setProductName("테스트상품" + i);
            product.setPrice(1000 * i);
            product.setStock(100);
            testProducts.add(productRepository.save(product));
        }

        // 테스트 주문 데이터 생성 (최근 3일 내)
        testOrderItems = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(1);

        for (int i = 0; i < testProducts.size(); i++) {
            Product product = testProducts.get(i);
            // 상위 5개 상품이 더 많이 팔리도록 설정
            int salesCount = i < 5 ? (10 - i) * 10 : 5;

            for (int j = 0; j < salesCount; j++) {
                OrderItem orderItem = new OrderItem(
                        product.getProductId(),
                        1,
                        product.getPrice(),
                        baseTime.plusMinutes(j * 10) // 10분 간격으로 주문
                );
                orderItem.assignToOrder((long)(j / 5 + 1)); // 5개씩 묶어서 주문
                testOrderItems.add(orderItemRepository.save(orderItem));
            }
        }
    }

    private void createLargeOrderData() {
        // 추가로 대용량 주문 데이터 생성 (성능 테스트용)
        LocalDateTime baseTime = LocalDateTime.now().minusDays(2);

        for (int i = 0; i < 1000; i++) {
            Product randomProduct = testProducts.get(i % testProducts.size());
            OrderItem orderItem = new OrderItem(
                    randomProduct.getProductId(),
                    1,
                    randomProduct.getPrice(),
                    baseTime.plusMinutes(i)
            );
            orderItem.assignToOrder((long)(i / 10 + 100)); // 주문 ID를 다양하게 설정
            orderItemRepository.save(orderItem);
        }
    }

    private void clearAllCaches() {
        if (cacheManager.getCache("topProducts") != null) {
            cacheManager.getCache("topProducts").clear();
        }
        // Redis 전체 플러시 (테스트 환경에서만 사용)
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Redis 연결 실패 시 무시
            System.out.println("Redis flush 실패 (테스트 환경에서는 정상): " + e.getMessage());
        }
    }

    private void clearCache(String cacheName) {
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).clear();
        }
    }
}