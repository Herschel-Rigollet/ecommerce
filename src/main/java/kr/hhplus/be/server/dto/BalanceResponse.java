package kr.hhplus.be.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "잔액 응답 DTO")
public class BalanceResponse {
    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "현재 잔액")
    private Long balance;

    public BalanceResponse(Long userId, Long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBalance() {
        return balance;
    }
}
