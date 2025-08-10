package kr.hhplus.be.server.user.infrastructure.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 잔액 관리: 비관적 락
public interface UserJpaRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
