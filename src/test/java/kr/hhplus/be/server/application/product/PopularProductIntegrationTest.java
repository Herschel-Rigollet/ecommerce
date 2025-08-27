package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.presentation.dto.response.PopularProductResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PopularProductIntegrationTest {

    // Redis 테스트 컨테이너 설정
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true); // 테스트 간 컨테이너 재사용으로 성능 향상

    // 테스트 컨테이너의 Redis 정보를 Spring에 동적 주입
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Product product1, product2, product3, product4, product5;

    @BeforeEach
    void setUp() {
        // 독립적 테스트 환경 보장: Redis 초기화
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();

        // 테스트용 상품 데이터 생성 (Top5 테스트를 위해 5개 이상 생성)
        product1 = createAndSaveProduct("베스트셀러 상품", 15000, 100);
        product2 = createAndSaveProduct("인기 상품", 25000, 80);
        product3 = createAndSaveProduct("신상품", 35000, 60);
        product4 = createAndSaveProduct("할인 상품", 12000, 40);
        product5 = createAndSaveProduct("한정판 상품", 50000, 20);
    }

    @Test
    @Order(1)
    @DisplayName("Redis 연결 상태 및 초기 환경 검증")
    void redisConnectionAndInitialStateTest() {
        // Redis 컨테이너 정상 실행 확인
        assertThat(redis.isRunning()).isTrue();

        // 초기 상태에서 Redis 데이터 없음 확인
        List<PopularProductResponse> emptyResult = productService.getPopularProductsFromRedis();
        assertThat(emptyResult).isEmpty();

        // DB 폴백 기능 정상 동작 확인
        List<PopularProductResponse> dbResult = productService.getTop5PopularProducts();
        assertThat(dbResult).isEmpty(); // 아직 주문 데이터 없으므로 빈 결과

        log.info("Redis 연결 및 초기 환경 검증 완료");
    }

    @Test
    @Order(2)
    @DisplayName("★ 핵심 기능: Redis 파이프라인 기반 인기상품 데이터 업데이트 검증 ★")
    void updatePopularProductsDataWithPipelineTest() {
        // Given: 다양한 주문 패턴으로 주문 아이템 생성
        List<OrderItem> orderItems = List.of(
                new OrderItem(product1.getProductId(), 10, 15000), // 상품1: 10개 (최고 판매량)
                new OrderItem(product2.getProductId(), 7, 25000),  // 상품2: 7개
                new OrderItem(product3.getProductId(), 5, 35000),  // 상품3: 5개
                new OrderItem(product1.getProductId(), 3, 15000),  // 상품1: 추가 3개 (총 13개)
                new OrderItem(product4.getProductId(), 2, 12000),  // 상품4: 2개
                new OrderItem(product2.getProductId(), 1, 25000)   // 상품2: 추가 1개 (총 8개)
        );

        // When: Redis 파이프라인을 통한 데이터 업데이트
        productService.updatePopularProductsData(orderItems);

        // Then: Redis에 정확한 판매량이 누적되었는지 검증
        String todayKey = "popular_products:" + LocalDate.now().toString();

        // 상품별 누적 판매량 검증
        Double score1 = stringRedisTemplate.opsForZSet().score(todayKey, product1.getProductId().toString());
        assertThat(score1).isEqualTo(13.0); // 10 + 3 = 13개

        Double score2 = stringRedisTemplate.opsForZSet().score(todayKey, product2.getProductId().toString());
        assertThat(score2).isEqualTo(8.0);  // 7 + 1 = 8개

        Double score3 = stringRedisTemplate.opsForZSet().score(todayKey, product3.getProductId().toString());
        assertThat(score3).isEqualTo(5.0);  // 5개

        Double score4 = stringRedisTemplate.opsForZSet().score(todayKey, product4.getProductId().toString());
        assertThat(score4).isEqualTo(2.0);  // 2개

        // TTL 설정 확인 (메모리 관리)
        Long ttl = stringRedisTemplate.getExpire(todayKey);
        assertThat(ttl).isGreaterThan(0);

        log.info("Redis 파이프라인 업데이트 검증 완료: 상품별 누적 판매량 정확");
    }

    @Test
    @Order(3)
    @DisplayName("★ 핵심 기능: 다중 날짜 데이터 집계 및 Top5 순위 정확성 검증 ★")
    void multiDayAggregationAndTop5RankingTest() {
        // Given: 3일간의 현실적인 판매 데이터 시뮬레이션
        setupRealistic3DaysData();

        // When: Redis SortedSet 기반 Top5 조회
        List<PopularProductResponse> top5Results = productService.getPopularProductsFromRedis();

        // Then: 정확한 순위와 집계 결과 검증
        assertThat(top5Results).hasSize(5); // Top5 개수 정확

        // 1위: 상품1 (총 30개: 오늘 15 + 어제 10 + 그저께 5)
        PopularProductResponse first = top5Results.get(0);
        assertThat(first.getProductId()).isEqualTo(product1.getProductId());
        assertThat(first.getTotalSold()).isEqualTo(30);
        assertThat(first.getProductName()).isEqualTo("베스트셀러 상품");

        // 2위: 상품2 (총 21개: 오늘 12 + 어제 7 + 그저께 2)
        PopularProductResponse second = top5Results.get(1);
        assertThat(second.getProductId()).isEqualTo(product2.getProductId());
        assertThat(second.getTotalSold()).isEqualTo(21);

        // 3위: 상품3 (총 15개: 오늘 8 + 어제 5 + 그저께 2)
        PopularProductResponse third = top5Results.get(2);
        assertThat(third.getProductId()).isEqualTo(product3.getProductId());
        assertThat(third.getTotalSold()).isEqualTo(15);

        // 4위: 상품4 (총 10개: 오늘 6 + 어제 3 + 그저께 1)
        PopularProductResponse fourth = top5Results.get(3);
        assertThat(fourth.getProductId()).isEqualTo(product4.getProductId());
        assertThat(fourth.getTotalSold()).isEqualTo(10);

        // 5위: 상품5 (총 5개: 오늘 3 + 어제 2 + 그저께 0)
        PopularProductResponse fifth = top5Results.get(4);
        assertThat(fifth.getProductId()).isEqualTo(product5.getProductId());
        assertThat(fifth.getTotalSold()).isEqualTo(5);

        log.info("다중 날짜 집계 및 Top5 순위 검증 완료: 순위 정확성 확인");
    }

    @Test
    @Order(4)
    @DisplayName("★ 동시성 검증: 대용량 동시 주문에서도 정확한 집계 처리 ★")
    void highConcurrencyOrderProcessingTest() throws InterruptedException {
        // Given: 대용량 동시 주문 시뮬레이션
        int threadCount = 20;      // 20개 스레드
        int ordersPerThread = 10;  // 스레드당 10개 주문
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When: 동시에 여러 스레드에서 주문 데이터 업데이트
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        List<OrderItem> concurrentOrders = List.of(
                                new OrderItem(product1.getProductId(), 1, 15000),
                                new OrderItem(product2.getProductId(), 2, 25000)
                        );
                        productService.updatePopularProductsData(concurrentOrders);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await();
        executor.shutdown();

        // Then: 동시성 환경에서도 정확한 집계 결과 검증
        String todayKey = "popular_products:" + LocalDate.now().toString();

        // 상품1: 총 200개 (20 스레드 * 10 주문 * 1개씩)
        Double finalScore1 = stringRedisTemplate.opsForZSet().score(todayKey, product1.getProductId().toString());
        assertThat(finalScore1).isEqualTo(200.0);

        // 상품2: 총 400개 (20 스레드 * 10 주문 * 2개씩)
        Double finalScore2 = stringRedisTemplate.opsForZSet().score(todayKey, product2.getProductId().toString());
        assertThat(finalScore2).isEqualTo(400.0);

        log.info("고동시성 환경 검증 완료: 200개 스레드에서 정확한 집계 처리");
    }

    private Product createAndSaveProduct(String name, int price, int stock) {
        Product product = new Product();
        product.setProductName(name);
        product.setPrice(price);
        product.setStock(stock);
        return productRepository.save(product);
    }

    /**
     * 현실적인 3일간 판매 데이터 설정
     * 베스트셀러 > 인기 상품 > 신상품 > 할인 상품 > 한정판 순으로 설정
     */
    private void setupRealistic3DaysData() {
        LocalDate today = LocalDate.now();

        // 오늘 판매 데이터 (가장 많은 판매량)
        String todayKey = "popular_products:" + today.toString();
        stringRedisTemplate.opsForZSet().add(todayKey, product1.getProductId().toString(), 15.0); // 베스트셀러
        stringRedisTemplate.opsForZSet().add(todayKey, product2.getProductId().toString(), 12.0); // 인기 상품
        stringRedisTemplate.opsForZSet().add(todayKey, product3.getProductId().toString(), 8.0);  // 신상품
        stringRedisTemplate.opsForZSet().add(todayKey, product4.getProductId().toString(), 6.0);  // 할인 상품
        stringRedisTemplate.opsForZSet().add(todayKey, product5.getProductId().toString(), 3.0);  // 한정판

        // 어제 판매 데이터 (중간 판매량)
        String yesterdayKey = "popular_products:" + today.minusDays(1).toString();
        stringRedisTemplate.opsForZSet().add(yesterdayKey, product1.getProductId().toString(), 10.0);
        stringRedisTemplate.opsForZSet().add(yesterdayKey, product2.getProductId().toString(), 7.0);
        stringRedisTemplate.opsForZSet().add(yesterdayKey, product3.getProductId().toString(), 5.0);
        stringRedisTemplate.opsForZSet().add(yesterdayKey, product4.getProductId().toString(), 3.0);
        stringRedisTemplate.opsForZSet().add(yesterdayKey, product5.getProductId().toString(), 2.0);

        // 그저께 판매 데이터 (적은 판매량)
        String dayBeforeKey = "popular_products:" + today.minusDays(2).toString();
        stringRedisTemplate.opsForZSet().add(dayBeforeKey, product1.getProductId().toString(), 5.0);
        stringRedisTemplate.opsForZSet().add(dayBeforeKey, product2.getProductId().toString(), 2.0);
        stringRedisTemplate.opsForZSet().add(dayBeforeKey, product3.getProductId().toString(), 2.0);
        stringRedisTemplate.opsForZSet().add(dayBeforeKey, product4.getProductId().toString(), 1.0);
        // product5는 그저께 판매 없음 (0개)

        // 총합 계산:
        // 상품1: 15 + 10 + 5 = 30개 (1위)
        // 상품2: 12 + 7 + 2 = 21개 (2위)
        // 상품3: 8 + 5 + 2 = 15개 (3위)
        // 상품4: 6 + 3 + 1 = 10개 (4위)
        // 상품5: 3 + 2 + 0 = 5개 (5위)
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PopularProductIntegrationTest.class);
}