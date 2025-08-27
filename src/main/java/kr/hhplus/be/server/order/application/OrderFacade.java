package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.order.presentation.dto.response.OrderResponse;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.order.presentation.dto.request.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final StockRollbackService stockRollbackService;
    private final CouponService couponService;

    /**
     * 주문 처리에 상품별 멀티락 적용
     *
     * 멀티락 동작:
     * 1. request.items에서 productId 리스트 추출: [1, 3, 2]
     * 2. 정렬하여 데드락 방지: [1, 2, 3]
     * 3. 각 상품별로 락 키 생성: ["MULTI_LOCK:1", "MULTI_LOCK:2", "MULTI_LOCK:3"]
     * 4. 순서대로 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 종료 → 락 해제
     */
    @DistributedLock(
            key = "#request.items.![productId]", // 상품 ID 리스트 추출
            multiLock = true,
            keyPrefix = "STOCK:",
            waitTime = 10L,
            leaseTime = 30L,
            failMessage = "선택한 상품들이 다른 주문에서 처리 중입니다. 잠시 후 다시 시도해주세요."
    )
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("주문 처리 시작: userId={}, 상품수={} (상품별 멀티락 획득됨, 트랜잭션 시작됨)",
                request.getUserId(), request.getItems().size());

        // 1. 사용자 조회 (비관적 락 적용)
        User user = userService.getPointByUserIdForUpdate(request.getUserId());

        // 2. 주문 아이템 처리 (멀티락으로 이미 보호됨 - 일반 조회 사용 가능)
        List<OrderItem> orderItems = new ArrayList<>();
        List<Product> processedProducts = new ArrayList<>();

        try {
            for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                // 일반 조회 (멀티락이 이미 상품별 동시성 보장)
                Product product = productService.getProductById(itemRequest.getProductId());

                if (product.getStock() < itemRequest.getQuantity()) {
                    throw new IllegalStateException(
                            String.format("재고가 부족한 상품입니다: %s (현재 재고: %d, 요청 수량: %d)",
                                    product.getProductName(), product.getStock(), itemRequest.getQuantity())
                    );
                }

                // 재고 차감 (멀티락으로 동시성 보장됨)
                product.decreaseStock(itemRequest.getQuantity());
                productService.saveProduct(product); // 명시적 저장
                processedProducts.add(product);

                OrderItem item = new OrderItem(itemRequest.getProductId(), itemRequest.getQuantity(), product.getPrice());
                orderItems.add(item);

                log.info("상품 재고 차감 완료: productId={}, quantity={}, 남은재고={}",
                        itemRequest.getProductId(), itemRequest.getQuantity(), product.getStock());
            }

            // 3. 총 주문 금액 계산
            int totalAmount = orderItems.stream()
                    .mapToInt(OrderItem::getTotalPrice)
                    .sum();

            // 4. 쿠폰 적용 (낙관적 락 사용)
            if (request.getCouponId() != null) {
                totalAmount = applyCouponDiscountOptimistic(request.getCouponId(), request.getUserId(), totalAmount);
            }

            // 5. 잔액 검증 및 차감
            if (user.getPoint() < totalAmount) {
                // 잔액 부족 시 재고 복구
                rollbackStock(orderItems, processedProducts);
                throw new IllegalStateException(
                        String.format("잔액이 부족합니다. (현재 잔액: %d원, 필요 금액: %d원)",
                                user.getPoint(), totalAmount)
                );
            }

            user.usePoint(totalAmount);
            log.info("포인트 차감 완료: userId={}, 차감액={}, 남은잔액={}",
                    request.getUserId(), totalAmount, user.getPoint());

            // 6. 주문 생성 및 저장
            Order savedOrder = orderService.saveOrder(user.getUserId(), orderItems);

            // 7. 인기상품 데이터 실시간 업데이트 (Redis)
            productService.updatePopularProductsData(orderItems);

            log.info("주문 생성 완료: orderId={} (트랜잭션 커밋 예정)", savedOrder.getOrderId());

            return OrderResponse.from(savedOrder, orderItems);

        } catch (Exception e) {
            log.error("주문 처리 실패 - 재고 롤백 시작: userId={}, 에러={} (트랜잭션 롤백 예정)",
                    request.getUserId(), e.getMessage());

            // 예외 발생 시 재고 복구
            rollbackStock(orderItems, processedProducts);
            throw e; // 원래 예외 재전파
        }
    }

    /**
     * 재고 롤백 (멀티락으로 이미 보호된 상태에서 처리)
     */
    private void rollbackStock(List<OrderItem> orderItems, List<Product> processedProducts) {
        try {
            for (OrderItem item : orderItems) {
                Product product = processedProducts.stream()
                        .filter(p -> p.getProductId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

                if (product != null) {
                    product.increaseStock(item.getQuantity());
                    productService.saveProduct(product); // 명시적 저장
                    log.info("재고 복구 완료: productId={}, 복구수량={}, 복구 후 재고={}",
                            product.getProductId(), item.getQuantity(), product.getStock());
                }
            }
        } catch (Exception rollbackException) {
            log.error("재고 복구 실패: {}", rollbackException.getMessage());
        }
    }

    private int applyCouponDiscountOptimistic(Long couponId, Long userId, int totalAmount) {
        return couponService.useCouponAndCalculateDiscount(couponId, userId, totalAmount);
    }
}
