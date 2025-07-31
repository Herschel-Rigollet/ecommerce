package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductRepository;
import kr.hhplus.be.server.application.user.UserRepository;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.presentation.order.request.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 주문 아이템 처리
        List<OrderItem> orderItems = request.getItems().stream().map(itemRequest -> {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            // 2-1. 재고 확인
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalStateException("재고가 부족한 상품입니다: " + product.getName());
            }

            // 2-2. 재고 차감
            product.decreaseStock(itemRequest.getQuantity());

            return new OrderItem(product.getId(), itemRequest.getQuantity(), product.getPrice());
        }).collect(toList());

        // 3. 총 주문 금액 계산
        int totalAmount = orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();

        // 4. 사용자 잔액 차감
        user.usePoint(totalAmount);

        // 5. 주문 생성 및 저장
        Order order = new Order(user.getId(), orderItems);
        return orderRepository.save(order);
    }
}
