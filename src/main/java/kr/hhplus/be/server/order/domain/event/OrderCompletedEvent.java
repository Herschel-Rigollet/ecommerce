package kr.hhplus.be.server.order.domain.event;

import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderCompletedEvent {
    private final Long orderId;
    private final Long userId;
    private final List<OrderItemData> orderItems;
    private final int totalAmount;
    private final Long couponId;
    private final LocalDateTime orderTime;
    private final String eventId; // 멱등성 보장을 위한 고유 ID

    public OrderCompletedEvent(Long orderId, Long userId, List<OrderItem> orderItems,
                               int totalAmount, Long couponId) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderItems = orderItems.stream()
                .map(item -> new OrderItemData(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        this.totalAmount = totalAmount;
        this.couponId = couponId;
        this.orderTime = LocalDateTime.now();
        this.eventId = generateEventId();
    }

    private String generateEventId() {
        return "ORDER_" + orderId + "_" + System.nanoTime();
    }

    @Getter
    public static class OrderItemData {
        private final Long productId;
        private final int quantity;
        private final int price;

        public OrderItemData(Long productId, int quantity, int price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }

        public int getTotalPrice() {
            return price * quantity;
        }
    }
}