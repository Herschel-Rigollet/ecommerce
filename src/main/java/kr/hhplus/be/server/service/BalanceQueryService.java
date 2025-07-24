package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import org.springframework.stereotype.Service;

@Service
public class BalanceQueryService {
    private final BalanceRepository repository;

    public BalanceQueryService(BalanceRepository repository) {
        this.repository = repository;
    }

    public User getBalance(Long userId) {
        return repository.findByUserId(userId);
    }
}
