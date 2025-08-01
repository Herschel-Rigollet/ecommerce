package kr.hhplus.be.server.presentation.coupon.response;

import kr.hhplus.be.server.domain.coupon.Coupon;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponResponse {
    private Long couponId;
    private String code;
    private int discountRate;
    private boolean used;

    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
                .couponId(coupon.getId())
                .code(coupon.getCode())
                .discountRate(coupon.getDiscountRate())
                .used(coupon.isUsed())
                .build();
    }
}
