package kr.hhplus.be.server.user.domain.repository;

import kr.hhplus.be.server.user.domain.User;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long userId);
    User save(User user);
    Optional<User> findByIdForUpdate(Long userId);
}
