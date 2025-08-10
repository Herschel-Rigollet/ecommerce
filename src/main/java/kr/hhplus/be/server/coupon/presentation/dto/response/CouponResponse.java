package kr.hhplus.be.server.coupon.presentation.dto.response;

import kr.hhplus.be.server.coupon.domain.Coupon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponResponse {
    private Long couponId;
    private String code;
    private int discountRate;
    private boolean used;
    private LocalDateTime issuedAt;
    private LocalDateTime expirationDate;

    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
                .couponId(coupon.getCouponId())
                .code(coupon.getCode())
                .discountRate(coupon.getDiscountRate())
                .used(coupon.isUsed())
                .issuedAt(coupon.getIssuedAt())
                .expirationDate(coupon.getExpirationDate())
                .build();
    }
}
