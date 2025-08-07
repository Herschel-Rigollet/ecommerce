package kr.hhplus.be.server.user.application;

import jakarta.persistence.OptimisticLockException;
import kr.hhplus.be.server.user.domain.repository.UserRepository;
import kr.hhplus.be.server.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void charge(Long userId, long amount) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자를 찾을 수 없습니다."));

        user.charge(amount);
        userRepository.save(user);
    }

    // 비관적 락으로 사용자 조회
    @Transactional
    public User getPointByUserIdForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자를 찾을 수 없습니다: " + userId));
    }

    // 동시성 안전한 포인트 차감
    @Transactional
    public void usePoint(Long userId, long amount) {
        // 비관적 락으로 사용자 조회
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 잔액 검증 및 차감
        if (user.getPoint() < amount) {
            throw new IllegalStateException("포인트가 부족합니다. 현재 잔액: " + user.getPoint());
        }

        user.usePoint(amount);
        // 트랜잭션 끝나면 자동으로 락 해제
    }

    @Transactional(readOnly = true)
    public User getPointByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자를 찾을 수 없습니다."));
    }

    // 잔액 충전 시 낙관적 락 적용
    @Transactional
    public void chargeOptimistic(Long userId, long amount) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

            user.charge(amount);
            userRepository.save(user); // OptimisticLockException 가능

        } catch (OptimisticLockException e) {
            throw new IllegalStateException("충전 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }
}
