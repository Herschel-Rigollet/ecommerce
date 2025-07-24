package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BalanceRollbackServiceTest {
    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private BalanceRollbackService balanceRollbackService;

    @Test
    @DisplayName("결제 실패 시 잔액이 정상 복구된다")
    void rollbackBalance_success() {
        // Given
        Long userId = 5L;
        long refundAmount = 2000L;
        User balance = new User(userId, 1000L);

        given(balanceRepository.findByUserId(userId)).willReturn(balance);

        // When
        balanceRollbackService.rollback(userId, refundAmount);

        // Then
        assertEquals(3000L, balance.getAmount());
        verify(balanceRepository).save(balance);
    }
}
