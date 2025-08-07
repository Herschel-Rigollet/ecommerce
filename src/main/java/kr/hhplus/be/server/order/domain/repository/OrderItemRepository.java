package kr.hhplus.be.server.order.domain.repository;

import kr.hhplus.be.server.order.domain.OrderItem;

import java.util.List;

public interface OrderItemRepository {
    OrderItem save(OrderItem orderItem);
    List<OrderItem> saveAll(List<OrderItem> orderItems);
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByProductIdAndOrderDateBetween(Long productId,
                                                       java.time.LocalDateTime start,
                                                       java.time.LocalDateTime end);
}
