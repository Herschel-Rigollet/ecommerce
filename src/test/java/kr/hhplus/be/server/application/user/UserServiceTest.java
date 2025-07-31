package kr.hhplus.be.server.application.user;

import kr.hhplus.be.server.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 정상적으로_포인트를_충전할_수_있다() {
        // Given
        Long userId = 1L;
        User existingUser = new User(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // When
        userService.charge(userId, 1000);

        // Then
        assertEquals(1000, existingUser.getPoint());
        verify(userRepository).save(existingUser);
    }

    @Test
    void 존재하지_않는_유저에게_포인트_충전은_실패해야_한다() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            userService.charge(userId, 2000);
        });

        // 충전(save)도 호출되지 않아야 함
        verify(userRepository, never()).save(any());
    }

    @Test
    void 포인트를_여러번_충전하면_누적된다() {
        // Given
        Long userId = 3L;
        User user = new User(userId);
        user.charge(500);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.charge(userId, 700);

        // Then
        assertEquals(1200, user.getPoint());
    }

    @Test
    void 음수_또는_0원_충전은_실패해야_한다() {
        // Given
        Long userId = 4L;
        User user = new User(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.charge(userId, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            userService.charge(userId, -100);
        });
    }

    @Test
    void 존재하지_않는_유저의_포인트_조회를_하면_예외가_발생한다() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            userService.getPointByUserId(userId);
        });
    }

    @Test
    void 정상적으로_포인트를_사용할_수_있다() {
        // Given
        User user = new User(5L);
        user.charge(1000);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        // When
        userService.usePoint(5L, 500);

        // Then
        assertEquals(500, user.getPoint());
        verify(userRepository).save(user);
    }

    @Test
    void 포인트가_부족하면_사용은_실패한다() {
        // Given
        User user = new User(6L);
        user.charge(300);
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(IllegalStateException.class, () -> userService.usePoint(6L, 1000));
    }

}
