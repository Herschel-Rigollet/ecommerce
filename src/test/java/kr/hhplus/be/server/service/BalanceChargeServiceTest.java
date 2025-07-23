package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BalanceChargeServiceTest {
    private BalanceRepository repository;
    private BalanceChargeService chargeService;

    @BeforeEach
    void setUp() {
        repository = mock(BalanceRepository.class);
        chargeService = new BalanceChargeService(repository);
    }

    @Test
    void 잔액을_정상적으로_충전할_수_있다() {
        // Given
        Long userId = 1L;
        User userBalance = new User(userId, 1000L);
        when(repository.findByUserId(userId)).thenReturn(userBalance);

        // When
        chargeService.charge(userId, 500L);

        // Then
        assertEquals(1500L, userBalance.getAmount());

        // 저장됐는지도 확인
        verify(repository, times(1)).save(userBalance);
    }
}
