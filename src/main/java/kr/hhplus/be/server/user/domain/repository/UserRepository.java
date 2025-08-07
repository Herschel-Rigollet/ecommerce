package kr.hhplus.be.server.user.domain.repository;

import kr.hhplus.be.server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long userId);

    void save(User user);
}
