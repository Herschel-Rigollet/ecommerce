package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BalanceRollbackService {
    private final BalanceRepository balanceRepository;

    public void rollback(Long userId, long amount) {
        User balance = balanceRepository.findByUserId(userId);
        balance.refund(amount);
        balanceRepository.save(balance);
    }
}
