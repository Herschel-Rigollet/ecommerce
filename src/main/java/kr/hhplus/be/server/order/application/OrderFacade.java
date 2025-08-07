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

            // 4. 쿠폰 적용 (있으면)
            if (request.getCouponId() != null) {
                totalAmount = applyCouponDiscount(request.getCouponId(), request.getUserId(), totalAmount);
            }

            // 5. 잔액 부족 시 재고 복구 후 예외
            if (user.getPoint() < totalAmount) {
                // 재고 복구
                rollbackStockSafely(rollbackTargets, lockedProducts);
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
                rollbackStockSafely(rollbackTargets, lockedProducts);
            }
            throw e; // 원래 예외 재전파
        }
    }

    // 동시성 안전한 재고 복구
    // 이미 락을 획득한 상품들에 대해 재고를 복구함
    private void rollbackStockSafely(List<OrderItem> rollbackTargets, List<Product> lockedProducts) {
        try {
            // 이미 락을 획득한 상품들이므로 안전하게 재고 복구 가능
            for (OrderItem item : rollbackTargets) {
                Product product = lockedProducts.stream()
                        .filter(p -> p.getProductId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

                if (product != null) {
                    product.increaseStock(item.getQuantity());
                    System.out.println("재고 복구: 상품ID " + product.getProductId() +
                            ", 복구 수량: " + item.getQuantity() +
                            ", 복구 후 재고: " + product.getStock());
                }
            }
        } catch (Exception rollbackException) {
            // 재고 복구 실패는 로깅만 함, 원래 예외는 그대로 전파
            System.err.println("재고 복구 실패: " + rollbackException.getMessage());
        }
    }

    private int applyCouponDiscount(Long couponId, Long userId, int totalAmount) {
        Coupon coupon = couponService.getCouponById(couponId);

        if (!coupon.getUserId().equals(userId)) {
            throw new IllegalStateException("해당 쿠폰은 이 사용자 소유가 아닙니다.");
        }
        if (coupon.isUsed()) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }

        // 할인 적용
        int discountedAmount = totalAmount - (totalAmount * coupon.getDiscountRate() / 100);

        // 쿠폰 사용 처리
        coupon.use();
        couponService.saveCoupon(coupon);

        return discountedAmount;
    }
}
