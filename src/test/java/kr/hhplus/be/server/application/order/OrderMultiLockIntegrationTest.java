package kr.hhplus.be.server.application.order;


import kr.hhplus.be.server.order.application.OrderFacade;
import kr.hhplus.be.server.order.presentation.dto.request.OrderRequest;
import kr.hhplus.be.server.order.presentation.dto.response.OrderResponse;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.user.domain.repository.UserRepository;
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

@SpringBootTest
@Testcontainers
@SqlGroup(@Sql(scripts = {"/sql/cleanup-test-data.sql", "/sql/order-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD))
@Slf4j
public class OrderMultiLockIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("주문 멀티락 - 1000명이 동시에 같은 상품 주문 (재고 100개)")
    void 멀티락_1000명_동시_같은상품주문_재고정확성() throws Exception {
        // Given
        int requestCount = 1000;
        int expectedSuccessCount = 100; // 상품1 재고가 100개
        Long targetProductId = 1L;

        // 초기 데이터 확인
        Product initialProduct = productRepository.findById(targetProductId).orElseThrow();
        assertThat(initialProduct.getStock()).isEqualTo(100);

        log.info("테스트 시작 - 상품1 초기재고: {}, 총 요청: {}", initialProduct.getStock(), requestCount);

        // When
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        List<Long> successOrderIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < requestCount; i++) {
            final int requestIndex = i;
            final Long userId = (long) (i % 3 + 1); // 사용자 1,2,3 순환

            executor.submit(() -> {
                try {
                    startLatch.await();

                    List<OrderRequest.OrderItemRequest> items = List.of(
                            new OrderRequest.OrderItemRequest(targetProductId, 1)
                    );
                    OrderRequest orderRequest = new OrderRequest(userId, items);

                    OrderResponse response = orderFacade.placeOrder(orderRequest);
                    successCount.incrementAndGet();
                    successOrderIds.add(response.getOrderId());

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("Request-" + requestIndex + " (User-" + userId + "): " + e.getMessage());
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
        assertThat(successCount.get()).isEqualTo(expectedSuccessCount);
        assertThat(failCount.get()).isEqualTo(requestCount - expectedSuccessCount);

        // 재고 검증
        Product finalProduct = productRepository.findById(targetProductId).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(0);

        // 주문 ID 중복 검증
        Set<Long> uniqueOrderIds = new HashSet<>(successOrderIds);
        assertThat(uniqueOrderIds.size()).isEqualTo(successCount.get());

        // 실패 에러 메시지 검증
        long stockErrors = errors.stream()
                .filter(error -> error.contains("재고가 부족"))
                .count();
        assertThat(stockErrors).isEqualTo(failCount.get());

        System.out.println("=== 주문 멀티락 1000명 동시 요청 테스트 결과 ===");
        System.out.println("총 요청: " + requestCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 재고: " + finalProduct.getStock());
        System.out.println("테스트 시간: " + (testEndTime - testStartTime) + "ms");
        System.out.println("처리량 (TPS): " + String.format("%.2f",
                (double) successCount.get() / ((testEndTime - testStartTime) / 1000.0)));

        executor.shutdown();
    }

    @Test
    @DisplayName("주문 멀티락 - 다중 상품 조합 동시 주문 (데드락 방지 검증)")
    void 멀티락_다중상품조합_동시주문_데드락방지() throws Exception {
        // Given
        int requestCount = 600; // 200 + 200 + 200

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger group1Success = new AtomicInteger(0); // 상품1,2 주문
        AtomicInteger group2Success = new AtomicInteger(0); // 상품2,3 주문
        AtomicInteger group3Success = new AtomicInteger(0); // 상품1,3 주문
        AtomicInteger totalFail = new AtomicInteger(0);

        // When - 3개 그룹이 서로 다른 상품 조합 주문
        for (int i = 0; i < requestCount; i++) {
            final int requestIndex = i;
            final int group = i % 3;
            final Long userId = 1L; // 충분한 포인트를 가진 사용자 사용

            executor.submit(() -> {
                try {
                    startLatch.await();

                    List<OrderRequest.OrderItemRequest> items;
                    if (group == 0) {
                        // 그룹1: 상품1,2 각 1개씩 (순서: 1 → 2)
                        items = List.of(
                                new OrderRequest.OrderItemRequest(1L, 1),
                                new OrderRequest.OrderItemRequest(2L, 1)
                        );
                    } else if (group == 1) {
                        // 그룹2: 상품2,3 각 1개씩 (순서: 2 → 3)
                        items = List.of(
                                new OrderRequest.OrderItemRequest(2L, 1),
                                new OrderRequest.OrderItemRequest(3L, 1)
                        );
                    } else {
                        // 그룹3: 상품3,1 각 1개씩 (순서: 3 → 1, 하지만 락은 1 → 3 순서로 정렬됨)
                        items = List.of(
                                new OrderRequest.OrderItemRequest(3L, 1),
                                new OrderRequest.OrderItemRequest(1L, 1)
                        );
                    }

                    OrderRequest orderRequest = new OrderRequest(userId, items);
                    OrderResponse response = orderFacade.placeOrder(orderRequest);

                    if (group == 0) group1Success.incrementAndGet();
                    else if (group == 1) group2Success.incrementAndGet();
                    else group3Success.incrementAndGet();

                } catch (Exception e) {
                    totalFail.incrementAndGet();
                    log.debug("그룹{} 주문 실패: 요청={}, 에러={}", group, requestIndex, e.getMessage());
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

        Product finalProduct1 = productRepository.findById(1L).orElseThrow();
        Product finalProduct2 = productRepository.findById(2L).orElseThrow();
        Product finalProduct3 = productRepository.findById(3L).orElseThrow();

        int totalSuccess = group1Success.get() + group2Success.get() + group3Success.get();

        System.out.println("=== 주문 멀티락 다중 상품 조합 테스트 결과 ===");
        System.out.println("그룹1 성공(상품1,2): " + group1Success.get());
        System.out.println("그룹2 성공(상품2,3): " + group2Success.get());
        System.out.println("그룹3 성공(상품3,1): " + group3Success.get());
        System.out.println("총 성공: " + totalSuccess);
        System.out.println("총 실패: " + totalFail.get());
        System.out.println("최종 재고 - 상품1: " + finalProduct1.getStock() +
                ", 상품2: " + finalProduct2.getStock() +
                ", 상품3: " + finalProduct3.getStock());
        System.out.println("테스트 시간: " + (testEndTime - testStartTime) + "ms");

        // 데드락 없이 정상 처리되었는지 확인
        assertThat(totalSuccess).isGreaterThan(0);
        assertThat(group1Success.get()).isGreaterThan(0);
        assertThat(group2Success.get()).isGreaterThan(0);
        assertThat(group3Success.get()).isGreaterThan(0);

        // 상품별 재고 검증 - 각 그룹에서 사용된 수량만큼 차감되어야 함
        int product1Used = group1Success.get() + group3Success.get();
        int product2Used = group1Success.get() + group2Success.get();
        int product3Used = group2Success.get() + group3Success.get();

        assertThat(finalProduct1.getStock()).isEqualTo(100 - product1Used);
        assertThat(finalProduct2.getStock()).isEqualTo(100 - product2Used);
        assertThat(finalProduct3.getStock()).isEqualTo(100 - product3Used);

        executor.shutdown();
    }

    @Test
    @DisplayName("주문 멀티락 - 포인트 부족 시 재고 롤백 검증")
    void 멀티락_포인트부족_재고롤백_검증() {
        // Given - 사용자999는 포인트 1000원만 보유
        Long poorUserId = 999L;

        List<OrderRequest.OrderItemRequest> items = List.of(
                new OrderRequest.OrderItemRequest(1L, 2), // 10000 * 2 = 20000원
                new OrderRequest.OrderItemRequest(2L, 1)  // 20000 * 1 = 20000원
                // 총 40000원 필요하지만 포인트는 1000원만 보유
        );
        OrderRequest orderRequest = new OrderRequest(poorUserId, items);

        // 초기 재고 확인
        Product originalProduct1 = productRepository.findById(1L).orElseThrow();
        Product originalProduct2 = productRepository.findById(2L).orElseThrow();
        int originalStock1 = originalProduct1.getStock();
        int originalStock2 = originalProduct2.getStock();

        // When & Then - 잔액 부족으로 주문 실패
        assertThatThrownBy(() -> orderFacade.placeOrder(orderRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액이 부족합니다");

        // 재고가 원래대로 복구되었는지 확인 (멀티락 롤백 검증)
        Product unchangedProduct1 = productRepository.findById(1L).orElseThrow();
        Product unchangedProduct2 = productRepository.findById(2L).orElseThrow();

        assertThat(unchangedProduct1.getStock()).isEqualTo(originalStock1);
        assertThat(unchangedProduct2.getStock()).isEqualTo(originalStock2);

        log.info("멀티락 롤백 검증 완료: 상품1 재고={}, 상품2 재고={}",
                unchangedProduct1.getStock(), unchangedProduct2.getStock());
    }

    @Test
    @DisplayName("주문 멀티락 - 성능 테스트 (처리량 측정)")
    void 멀티락_성능_테스트() throws Exception {
        // Given
        int requestCount = 50; // 성능 측정을 위한 적당한 수량

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            final Long userId = 1L;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    List<OrderRequest.OrderItemRequest> items = List.of(
                            new OrderRequest.OrderItemRequest(4L, 1) // 한정 상품 (재고 50개)
                    );
                    OrderRequest orderRequest = new OrderRequest(userId, items);

                    orderFacade.placeOrder(orderRequest);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 재고 부족 등의 실패는 정상
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        // Then
        long totalTime = testEndTime - testStartTime;
        double tps = (double) successCount.get() / (totalTime / 1000.0);

        System.out.println("=== 주문 멀티락 성능 테스트 결과 ===");
        System.out.println("성공한 주문: " + successCount.get());
        System.out.println("총 처리 시간: " + totalTime + "ms");
        System.out.println("처리량 (TPS): " + String.format("%.2f", tps));

        // 최소 성능 기준 검증
        assertThat(tps).isGreaterThan(2.0); // 최소 2 TPS 이상
        assertThat(successCount.get()).isEqualTo(50); // 재고 50개 모두 처리

        executor.shutdown();
    }
}