package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.CouponIssuePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CouponIssuePolicyTest {
    @Test
    void 최대_발급_개수를_초과하면_쿠폰을_발급할_수_없다() {
        // Given
        CouponIssuePolicy policy = new CouponIssuePolicy(3);
        policy.increaseIssued(); // 1
        policy.increaseIssued(); // 2
        policy.increaseIssued(); // 3

        // When & Then
        assertThrows(IllegalStateException.class, policy::increaseIssued);
    }
}
