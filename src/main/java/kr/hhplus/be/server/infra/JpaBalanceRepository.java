package kr.hhplus.be.server.infra;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;

public class JpaBalanceRepository implements BalanceRepository {
    private final SpringDataJpaBalanceRepository jpa;

    public JpaBalanceRepository(SpringDataJpaBalanceRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User findByUserId(Long userId) {
        return jpa.findById(userId)
                .orElse(new User(userId, 0L));  // 없으면 새로 생성
    }

    @Override
    public void save(User userBalance) {
        jpa.save(userBalance);
    }
}
