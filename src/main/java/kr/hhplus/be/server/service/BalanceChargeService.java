package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceChargeService {
    private final BalanceRepository repository;

    public BalanceChargeService(BalanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void charge(Long userId, long amount) {
        User balance = repository.findByUserId(userId);
        balance.charge(amount);
        repository.save(balance);
    }
}
