package kr.hhplus.be.server.order.infrastructure.repository;

import kr.hhplus.be.server.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // 상위 상품 조회를 위해 전체 판매 데이터 조회
    @Query("SELECT oi FROM OrderItem oi " +
            "JOIN Order o ON oi.orderId = o.orderId " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<OrderItem> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}