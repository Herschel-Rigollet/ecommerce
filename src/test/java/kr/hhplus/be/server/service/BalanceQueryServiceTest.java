package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BalanceQueryServiceTest {
    private BalanceRepository repository;
    private BalanceQueryService queryService;

    @BeforeEach
    void setUp() {
        repository = mock(BalanceRepository.class);
        queryService = new BalanceQueryService(repository);
    }

    @Test
    void 사용자ID로_잔액을_정상_조회한다() {
        // Given
        Long userId = 1L;
        User userBalance = new User(userId, 1000L);
        when(repository.findByUserId(userId)).thenReturn(userBalance);

        // When
        User result = queryService.getBalance(userId);

        // Then
        assertEquals(1000L, result.getAmount());
        assertEquals(userId, result.getUserId());
    }
}
