package kr.hhplus.be.server.dto;

public class OrderRequest {
    private Long userId;
    private Long productId;
    private int quantity;
    private String couponId;

    public OrderRequest() {}

    public OrderRequest(Long userId, Long productId, int quantity, String couponId) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.couponId = couponId;
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

    public String getCouponId() {
        return couponId;
    }
}
