package kr.hhplus.be.server.order.application;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final StockRollbackService stockRollbackService;
    private final CouponService couponService;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        // 1. 사용자 조회 (비관적 락 적용)
        User user = userService.getPointByUserIdForUpdate(request.getUserId());

        // 2. 주문 아이템 처리 (재고 확인 + 차감) - 비관적 락 적용
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItem> rollbackTargets = new ArrayList<>();
        List<Product> lockedProducts = new ArrayList<>(); // 락 획득한 상품들

        try {
            for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                // 비관적 락으로 상품 조회
                Product product = productService.getProductByIdForUpdate(itemRequest.getProductId());
                lockedProducts.add(product);

                if (product.getStock() < itemRequest.getQuantity()) {
                    throw new IllegalStateException("재고가 부족한 상품입니다: " + product.getProductName() +
                            " (현재 재고: " + product.getStock() + ", 요청 수량: " + itemRequest.getQuantity() + ")");
                }

                // 재고 차감 및 롤백 대상 등록
                product.decreaseStock(itemRequest.getQuantity());

                OrderItem item = new OrderItem(itemRequest.getProductId(), itemRequest.getQuantity(), product.getPrice());
                orderItems.add(item);
                rollbackTargets.add(item);
            }

            // 3. 총 주문 금액 계산
            int totalAmount = orderItems.stream()
                    .mapToInt(OrderItem::getTotalPrice)
                    .sum();

            // 4. 쿠폰 적용 (낙관적 락 사용)
            if (request.getCouponId() != null) {
                totalAmount = applyCouponDiscountOptimistic(request.getCouponId(), request.getUserId(), totalAmount);
            }

            // 5. 잔액 부족 시 재고 복구 후 예외
            if (user.getPoint() < totalAmount) {
                // 재고 복구
                stockRollbackService.rollbackSafely(rollbackTargets, lockedProducts);
                throw new IllegalStateException("잔액이 부족합니다. (현재 잔액: " + user.getPoint() + "원, 필요 금액: " + totalAmount + "원)");
            }

            // 6. 포인트 차감
            user.usePoint(totalAmount);

            // 7. 주문 생성 및 저장
            Order savedOrder = orderService.saveOrder(user.getUserId(), orderItems);

            // 8. OrderResponse 생성해서 반환
            return OrderResponse.from(savedOrder, orderItems);

        } catch (Exception e) {
            // 예외 발생 시 재고 복구 (트랜잭션 롤백과 별개로 명시적 복구)
            if (!rollbackTargets.isEmpty()) {
                stockRollbackService.rollbackSafely(rollbackTargets, lockedProducts);
            }
            throw e; // 원래 예외 재전파
        }
    }

    private int applyCouponDiscountOptimistic(Long couponId, Long userId, int totalAmount) {
        return couponService.useCouponAndCalculateDiscount(couponId, userId, totalAmount);
    }
}
