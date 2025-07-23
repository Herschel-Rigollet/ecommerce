package kr.hhplus.be.server.domain;

import org.junit.jupiter.api.Test;

public class UserBalanceTest {
    @Test
    void 잔액을_정상적으로_충전할_수_있다() {
        // Given
        UserBalance balance = new UserBalance(1L, 1000L);

        // When
        balance.charge(500L);

        // Then
        assertEquals(1500L, balance.getAmount());
    }

    @Test
    void 잔액_충전_금액이_0원_이하면_예외를_던진다() {
        UserBalance balance = new UserBalance(1L, 1000L);

        assertThrows(IllegalArgumentException.class, () -> balance.charge(0L));
        assertThrows(IllegalArgumentException.class, () -> balance.charge(-500L));
    }
}
