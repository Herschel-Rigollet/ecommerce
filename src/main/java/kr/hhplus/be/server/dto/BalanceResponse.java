package kr.hhplus.be.server.dto;

public class BalanceResponse {
    private Long userId;
    private long amount;

    public BalanceResponse(Long userId, long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public Long getUserId() { return userId; }
    public long getAmount() { return amount; }
}
