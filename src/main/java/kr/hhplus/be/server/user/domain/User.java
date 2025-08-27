package kr.hhplus.be.server.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "point", nullable = false)
    private long point;

    @Version
    @Column(name = "version")
    private Long version;

    public void charge(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        this.point += amount;
    }

    public void usePoint(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        if (this.point < amount) throw new IllegalStateException("포인트가 부족합니다.");
        this.point -= amount;
    }

}