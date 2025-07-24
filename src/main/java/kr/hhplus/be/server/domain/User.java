package kr.hhplus.be.server.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class User {
    private static final String INVALID_CHARGE_MESSAGE = "충전 금액은 0보다 커야 합니다.";
    private static final String INSUFFICIENT_BALANCE_MESSAGE = "잔액이 부족합니다.";

    @Id
    private final Long userId;
    private long amount;

    public User(Long userId, long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public void charge(long value) {
        validatePositive(value);
        this.amount += value;
    }

    public void use(long value) {
        validatePositive(value);
        if (this.amount < value) {
            throw new IllegalArgumentException(INSUFFICIENT_BALANCE_MESSAGE);
        }
        this.amount -= value;
    }

    private void validatePositive(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(INVALID_CHARGE_MESSAGE);
        }
    }

    public void refund(long amount) {
        this.amount += amount;
    }

    public long getAmount() {
        return amount;
    }

    public Long getUserId() {
        return userId;
    }
}
