package kr.hhplus.be.server.coupon.presentation.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CouponRequestEvent {
    private final Long userId;
    private final String couponCode;
    private final LocalDateTime requestTime;
    private final String requestId; // 멱등성 보장

    public static CouponRequestEvent create(Long userId, String couponCode) {
        return new CouponRequestEvent(
                userId,
                couponCode,
                LocalDateTime.now(),
                "REQ_" + userId + "_" + couponCode + "_" + System.nanoTime()
        );
    }
}