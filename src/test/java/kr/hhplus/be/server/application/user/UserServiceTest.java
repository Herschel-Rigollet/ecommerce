package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("잔액 충전 성공")
    void charge_Success() {
        // Given
        Long userId = 1L;
        long chargeAmount = 10000L;
        User user = createUser(userId, 5000L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        userService.charge(userId, chargeAmount);

        // Then
        assertThat(user.getPoint()).isEqualTo(15000L);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 충전 시 예외 발생")
    void charge_UserNotFound_ThrowsException() {
        // Given
        Long userId = 999L;
        long chargeAmount = 10000L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.charge(userId, chargeAmount))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_Success() {
        // Given
        Long userId = 2L;
        long useAmount = 3000L;
        User user = createUser(userId, 5000L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        userService.usePoint(userId, useAmount);

        // Then
        assertThat(user.getPoint()).isEqualTo(2000L);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("잔액 부족 시 포인트 사용 실패")
    void usePoint_InsufficientBalance_ThrowsException() {
        // Given
        Long userId = 3L;
        long useAmount = 10000L;
        User user = createUser(userId, 5000L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.usePoint(userId, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족합니다.");
    }

    private User createUser(Long userId, long point) {
        User user = new User();
        user.charge(point);
        return user;
    }
}