package kr.hhplus.be.server.infra;

import kr.hhplus.be.server.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaBalanceRepository extends JpaRepository<User, Long> {
}
