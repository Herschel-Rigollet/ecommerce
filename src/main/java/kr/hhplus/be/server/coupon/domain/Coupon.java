package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "discount_rate", nullable = false)
    private int discountRate;

    @Column(name = "used")
    private boolean used = false;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt = LocalDateTime.now();

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

    // 할인 금액 계산
    public int calculateDiscountedAmount(int originalAmount) {
        if (originalAmount <= 0) {
            throw new IllegalArgumentException("원래 금액은 0보다 커야 합니다.");
        }
        return originalAmount - (originalAmount * discountRate / 100);
    }
}
