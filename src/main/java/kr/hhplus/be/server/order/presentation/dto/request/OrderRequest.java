package kr.hhplus.be.server.order.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    private Long userId;
    private List<OrderItemRequest> items;
    private Long couponId;

    public OrderRequest(Long userId, List<OrderItemRequest> items) {
        this.userId = userId;
        this.items = items;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long productId;
        private int quantity;
    }
}