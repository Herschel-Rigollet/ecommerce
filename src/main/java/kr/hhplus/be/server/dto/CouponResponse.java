package kr.hhplus.be.server.dto;

import kr.hhplus.be.server.domain.Coupon;

import java.time.LocalDateTime;

public class CouponResponse {
    private String id;
    private long userId;
    private int discountAmount;
    private boolean used;
    private LocalDateTime issuedAt;

    public CouponResponse(String id, long userId, int discountAmount, boolean used, LocalDateTime issuedAt) {
        this.id = id;
        this.userId = userId;
        this.discountAmount = discountAmount;
        this.used = used;
        this.issuedAt = issuedAt;
    }

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getUserId(),
                coupon.getDiscountAmount(),
                coupon.isUsed(),
                coupon.getIssuedAt()
        );
    }

    public String getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
}
