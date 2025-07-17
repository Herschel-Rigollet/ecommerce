package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class OrderItemDto {

    @Schema(description = "상품 ID", example = "1001")
    private Long productId;

    @Schema(description = "수량", example = "2")
    private int quantity;

    public OrderItemDto() {}

    public OrderItemDto(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
