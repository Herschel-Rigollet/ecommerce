package kr.hhplus.be.server.user.infrastructure.repository;

import kr.hhplus.be.server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {
}
