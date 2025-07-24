package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import org.springframework.transaction.annotation.Transactional;

public class BalanceUseService {
    private BalanceRepository repository;

    public BalanceUseService(BalanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void use(Long userId, long amount) {
        User balance = repository.findByUserId(userId);
        balance.use(amount);  // 예외는 도메인에서 발생
        repository.save(balance);
    }
}
