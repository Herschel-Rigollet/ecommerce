package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    @Schema(description = "주문 ID", example = "501")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "총 결제 금액", example = "18000")
    private Long totalPrice;

    @Schema(description = "주문 시간", example = "2025-07-17T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "주문 상품 목록")
    private List<OrderItemDto> items;

    public OrderResponse() {}

    public OrderResponse(Long orderId, Long userId, Long totalPrice,
                         LocalDateTime createdAt, List<OrderItemDto> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
        this.items = items;
    }

    public OrderResponse(String s, long l) {
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Long getTotalPrice() { return totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItemDto> getItems() { return items; }
}
