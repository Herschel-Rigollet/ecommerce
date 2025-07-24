package kr.hhplus.be.server.domain;

public class Order {
    private final Long userId;
    private final Long productId;
    private final int quantity;
    private final long totalAmount;

    public Order(Long userId, Long productId, int quantity, long totalAmount) {
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
