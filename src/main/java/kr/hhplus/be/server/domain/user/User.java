package kr.hhplus.be.server.domain.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class User {
    @Id
    private Long id;

    private long point;

    public User(Long id) {
        this.id = id;
        this.point = 0;
    }

    public User(Long id, int amount) {
        this.id = id;
        this.point = amount;
    }

    public void charge(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        this.point += amount;
    }

    public void usePoint(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        if (this.point < amount) throw new IllegalStateException("포인트가 부족합니다.");
        this.point -= amount;
    }

    public long getPoint() {
        return point;
    }
}
