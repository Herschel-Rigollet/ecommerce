package kr.hhplus.be.server.domain;

public class User {
    private final Long userId;
    private long amount;

    public User(Long userId, long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public void charge(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.amount += value;
    }

    public long getAmount() {
        return amount;
    }

    public Long getUserId() {
        return userId;
    }
}
