package kr.hhplus.be.server.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserBalanceTest {
    @Test
    void 잔액을_정상적으로_충전할_수_있다() {
        // Given
        User balance = new User(1L, 1000L);

        // When
        balance.charge(500L);

        // Then
        assertEquals(1500L, balance.getAmount());
    }

    @Test
    void 잔액_충전_금액이_0원_이하면_예외를_던진다() {
        User balance = new User(1L, 1000L);

        assertThrows(IllegalArgumentException.class, () -> balance.charge(0L));
        assertThrows(IllegalArgumentException.class, () -> balance.charge(-500L));
    }

    @Test
    void 잔액을_정상적으로_차감할_수_있다() {
        // Given
        User balance = new User(1L, 1000L);

        // When
        balance.use(300L);

        // Then
        assertEquals(700L, balance.getAmount());
    }

    @Test
    void 잔액보다_많은_금액_차감시_예외가_발생한다() {
        User balance = new User(1L, 500L);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> balance.use(1000L));
    }
}
