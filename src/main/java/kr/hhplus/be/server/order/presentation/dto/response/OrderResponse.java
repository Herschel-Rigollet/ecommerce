package kr.hhplus.be.server.order.presentation.dto.response;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private Long userId;
    private int totalAmount;
    private LocalDateTime orderDate;
    private List<OrderItemResult> items;

    @Getter
    @Builder
    public static class OrderItemResult {
        private Long productId;
        private int quantity;
        private int unitPrice;
        private int totalPrice;

        public static OrderItemResult from(OrderItem orderItem) {
            return OrderItemResult.builder()
                    .productId(orderItem.getProductId())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .totalPrice(orderItem.getTotalPrice())
                    .build();
        }
    }

    public static OrderResponse from(Order order, List<OrderItem> orderItems) {
        List<OrderItemResult> itemResults = orderItems.stream()
                .map(OrderItemResult::from) // 수정!
                .toList();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .items(itemResults)
                .build();
    }
}