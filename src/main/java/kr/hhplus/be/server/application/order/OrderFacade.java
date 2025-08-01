package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.presentation.order.request.OrderRequest;
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
    public Order placeOrder(OrderRequest request) {
        // 1. 사용자 조회
        User user = userService.getPointByUserId(request.getUserId());

        // 2. 주문 아이템 처리 (재고 확인 + 차감)
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItem> rollbackTargets = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productService.getProductById(itemRequest.getProductId());

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalStateException("재고가 부족한 상품입니다: " + product.getName());
            }

            // 재고 차감 및 롤백 대상 등록
            product.decreaseStock(itemRequest.getQuantity());

            OrderItem item = new OrderItem(product.getId(), itemRequest.getQuantity(), product.getPrice());
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
            stockRollbackService.rollback(rollbackTargets);
            throw new IllegalStateException("잔액이 부족합니다.");
        }

        // 6. 포인트 차감
        user.usePoint(totalAmount);

        // 7. 주문 생성 및 저장
        return orderService.saveOrder(user.getId(), orderItems);
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
