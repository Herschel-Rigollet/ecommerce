package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "쿠폰 응답 DTO")
public class CouponResponse {
    @Schema(description = "쿠폰 ID", example = "101")
    private Long couponId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "할인율 (%)", example = "10.0")
    private double discountRate;

    @Schema(description = "사용 여부", example = "false")
    private boolean used;

    @Schema(description = "쿠폰 발급 일시", example = "2025-05-12T00:00:00")
    private LocalDateTime issuedAt;

    @Schema(description = "쿠폰 만료 일시", example = "2025-10-12T00:00:00")
    private LocalDateTime expiresAt;

    public CouponResponse() {}

    public CouponResponse(Long couponId, Long userId, double discountRate, boolean used,
                          LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.couponId = couponId;
        this.userId = userId;
        this.discountRate = discountRate;
        this.used = used;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
