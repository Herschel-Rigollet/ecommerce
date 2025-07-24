package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import kr.hhplus.be.server.infra.DataPlatformSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {
    private final ProductRepository productRepository;
    private final BalanceRepository balanceRepository;
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final DataPlatformSender sender;

    private final StockRollbackService stockRollbackService;
    private final CouponRollbackService couponRollbackService;
    private final BalanceRollbackService balanceRollbackService;

    public OrderResponse execute(OrderRequest request) {
        Long userId = request.getUserId();
        Long productId = request.getProductId();
        int quantity = request.getQuantity();
        Long couponId = Long.valueOf(request.getCouponId());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("재고가 부족합니다.");
        }

        long totalPrice = product.getPrice() * quantity;

        if (couponId != null) {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));
            if (coupon.isUsed()) {
                throw new IllegalStateException("이미 사용된 쿠폰입니다.");
            }
            totalPrice -= coupon.getDiscountAmount(); // 현재 메서드 기준
            coupon.markUsed(); // 실제 구현 여부는 체크 필요
            couponRepository.save(coupon);
        }

        User balance = balanceRepository.findByUserId(userId);
        if (balance.getAmount() < totalPrice) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }

        balance.use(totalPrice);
        balanceRepository.save(balance);

        product.decreaseStock(quantity);
        productRepository.save(product);

        Order order = Order.place(userId, productId, quantity, totalPrice, couponId);
        orderRepository.save(order);

        return new OrderResponse(order.getUserId(), order.getProductId(), order.getQuantity(), order.getTotalAmount());
    }

}
