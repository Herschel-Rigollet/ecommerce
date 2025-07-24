package kr.hhplus.be.server.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.Coupon;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
public class CouponEntity {
    @Id
    private String id;

    private long userId;

    private int discountAmount;

    private boolean used;

    private LocalDateTime issuedAt;

    public static CouponEntity from(Coupon domain) {
        CouponEntity entity = new CouponEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.discountAmount = domain.getDiscountAmount();
        entity.issuedAt = domain.getIssuedAt();
        entity.used = domain.isUsed();
        return entity;
    }

    public Coupon toDomain() {
        Coupon coupon = new Coupon(this.userId, this.discountAmount);
        if (this.used) coupon.markUsed();
        return coupon;
    }
}
