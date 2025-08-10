package kr.hhplus.be.server.order.domain.repository;

import kr.hhplus.be.server.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long orderId);
}
