package kr.hhplus.be.server.order.domain.repository;

import kr.hhplus.be.server.order.domain.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository {
    OrderItem save(OrderItem orderItem);
    List<OrderItem> saveAll(List<OrderItem> orderItems);
    List<OrderItem> findByOrderId(Long orderId);
    // 전체 상품의 특정 기간 판매 데이터 조회
    List<OrderItem> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
