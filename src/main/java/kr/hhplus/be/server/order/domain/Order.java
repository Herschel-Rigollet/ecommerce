package kr.hhplus.be.server.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order")
@Getter @Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long OrderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private int totalAmount;

    @Column(name = "items")
    private List<OrderItem> items = new ArrayList<>();

    public Order(Long userId, List<OrderItem> items) {
        this.userId = userId;
        this.items = items;
        this.totalAmount = items.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();

        for (OrderItem item : items) {
            item.setOrder(this);
        }
    }
}
