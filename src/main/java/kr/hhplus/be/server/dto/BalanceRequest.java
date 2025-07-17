package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 충전 요청 DTO")
public class BalanceRequest {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "충전 금액", example = "5000")
    private Long amount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
