package kr.hhplus.be.server.dto;

public class OrderResponse {
    private Long userId;
    private Long productId;
    private int quantity;
    private long totalAmount;

    public OrderResponse(Long userId, Long productId, int quantity, long totalAmount) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTotalAmount() {
        return totalAmount;
    }
}
