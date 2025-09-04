package kr.hhplus.be.server.coupon.presentation.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CouponResultEvent {
    private final Long userId;
    private final String couponCode;
    private final boolean success;
    private final String message;
    private final Long couponId; // 성공시에만
    private final LocalDateTime processedTime;

    public static CouponResultEvent success(Long userId, String couponCode, Long couponId) {
        return new CouponResultEvent(
                userId, couponCode, true, "쿠폰 발급 성공", couponId, LocalDateTime.now()
        );
    }

    public static CouponResultEvent failure(Long userId, String couponCode, String reason) {
        return new CouponResultEvent(
                userId, couponCode, false, reason, null, LocalDateTime.now()
        );
    }
}