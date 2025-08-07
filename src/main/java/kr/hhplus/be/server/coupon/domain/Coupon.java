package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "discount_rate", nullable = false)
    private int discountRate;

    @Column(name = "used")
    private boolean used = false;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;


    public boolean isUsed() {
        return this.used;
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    public void use() {
        if (this.used) throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        if (isExpired()) throw new IllegalStateException("만료된 쿠폰입니다.");
        this.used = true;
    }
}
