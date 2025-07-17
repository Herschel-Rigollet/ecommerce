package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class OrderRequest {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용할 쿠폰 ID (없으면 null)", example = "123")
    private Long couponId;

    @Schema(description = "주문 상품 목록")
    private List<OrderItemDto> items;

    public OrderRequest() {}

    public OrderRequest(Long userId, Long couponId, List<OrderItemDto> items) {
        this.userId = userId;
        this.couponId = couponId;
        this.items = items;
    }

    public Long getUserId() { return userId; }
    public Long getCouponId() { return couponId; }
    public List<OrderItemDto> getItems() { return items; }
}
