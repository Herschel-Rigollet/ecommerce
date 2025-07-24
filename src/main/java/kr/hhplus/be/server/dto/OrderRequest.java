package kr.hhplus.be.server.dto;

public class OrderRequest {
    private Long userId;
    private Long productId;
    private int quantity;

    public OrderRequest() {}

    public OrderRequest(Long userId, Long productId, int quantity) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
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
}
