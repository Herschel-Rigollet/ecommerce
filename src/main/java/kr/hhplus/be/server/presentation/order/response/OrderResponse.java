package kr.hhplus.be.server.presentation.order.response;

import kr.hhplus.be.server.domain.order.Order;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private Long userId;
    private int totalAmount;
    private List<OrderItemResult> items;

    @Getter
    @Builder
    public static class OrderItemResult {
        private Long productId;
        private int quantity;
        private int unitPrice;
        private int totalPrice;
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResult> items = order.getItems().stream()
                .map(item -> OrderItemResult.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .build();
    }
}