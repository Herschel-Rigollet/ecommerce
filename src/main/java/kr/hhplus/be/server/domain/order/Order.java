package kr.hhplus.be.server.domain.order;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Order {

    private Long id;
    private Long userId;
    private int totalAmount;
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
