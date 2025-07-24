package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BalanceUseServiceTest {
    private BalanceRepository repository;
    private BalanceUseService useService;

    @BeforeEach
    void setUp() {
        repository = mock(BalanceRepository.class);
        useService = new BalanceUseService(repository);
    }

    @Test
    void 잔액을_정상적으로_차감할_수_있다() {
        // Given
        Long userId = 1L;
        User balance = new User(userId, 1000L);
        when(repository.findByUserId(userId)).thenReturn(balance);

        // When
        useService.use(userId, 400L);

        // Then
        assertEquals(600L, balance.getAmount());
        verify(repository).save(balance);
    }

    @Test
    void 잔액보다_많은_금액을_차감하려하면_예외가_발생한다() {
        // Given
        Long userId = 1L;
        User balance = new User(userId, 300L);
        when(repository.findByUserId(userId)).thenReturn(balance);

        // Then
        assertThrows(IllegalArgumentException.class, () -> useService.use(userId, 1000L));
    }
}
