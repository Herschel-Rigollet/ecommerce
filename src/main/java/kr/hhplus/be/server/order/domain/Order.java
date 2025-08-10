package kr.hhplus.be.server.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private int totalAmount;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    public Order(Long userId, List<OrderItem> items) {
        this.userId = userId;
        this.totalAmount = items.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
        this.orderDate = LocalDateTime.now();
    }

    public void validateOrder() {
        if (this.totalAmount <= 0) {
            throw new IllegalStateException("주문 금액이 0 이하입니다.");
        }
    }
}
