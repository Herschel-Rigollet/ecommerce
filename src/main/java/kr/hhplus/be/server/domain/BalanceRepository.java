package kr.hhplus.be.server.domain;

public interface BalanceRepository {
    User findByUserId(Long userId);
    void save(User userBalance);
}
