package kr.hhplus.be.server.order.domain.repository;

import kr.hhplus.be.server.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository {
    Order save(Order order);
}
