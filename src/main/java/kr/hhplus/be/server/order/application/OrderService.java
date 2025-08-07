package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.domain.repository.OrderRepository;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order saveOrder(Long userId, List<OrderItem> orderItems) {
        // 1. 주문 생성 및 저장
        Order order = new Order(userId, orderItems);
        order.validateOrder();
        Order savedOrder = orderRepository.save(order);

        // 2. 주문 아이템들에 주문 ID 할당 후 저장
        for (OrderItem item : orderItems) {
            item.assignToOrder(savedOrder.getOrderId());
            item.validateOrderItem();
        }
        orderItemRepository.saveAll(orderItems);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order getOrderWithItems(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 필요시 OrderItem을 별도 조회
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        return order;
    }
}