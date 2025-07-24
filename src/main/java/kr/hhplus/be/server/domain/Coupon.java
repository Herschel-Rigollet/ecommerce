package kr.hhplus.be.server.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Coupon {
    private final String id;
    private final long userId;
    private final int discountAmount;
    private final LocalDateTime issuedAt;
    private boolean used;

    public Coupon(long userId, int discountAmount) {
        this.id = UUID.randomUUID().toString(); // 고유 쿠폰 ID 생성
        this.userId = userId;
        this.discountAmount = discountAmount;
        this.issuedAt = LocalDateTime.now();
        this.used = false;
    }

    public void markUsed() {
        this.used = true;
    }

    public void rollbackUse() {
        this.used = false;
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

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public boolean isUsed() {
        return used;
    }
}
