package kr.hhplus.be.server.domain.coupon;

import java.time.LocalDateTime;

public class Coupon {

    private Long id;
    private Long userId;
    private String code;
    private int discountRate;
    private boolean used;

    public Coupon(Long userId, String code, int discountRate) {
        this.userId = userId;
        this.code = code;
        this.discountRate = discountRate;
        this.used = false;
    }

    public boolean isUsed() {
        return this.used;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCode() {
        return code;
    }

    public int getDiscountRate() {
        return discountRate;
    }

    public void use() {
        if (this.used) throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        this.used = true;
    }
}
