package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponIssuePolicy;
import kr.hhplus.be.server.domain.CouponRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CouponIssueServiceTest {
    @Test
    void 쿠폰_발급_성공() {
        // Given
        long userId = 1L;
        int discount = 1000;

        CouponRepository couponRepository = mock(CouponRepository.class);
        CouponIssuePolicy policy = new CouponIssuePolicy(100);  // 최대 100장

        CouponIssueService service = new CouponIssueService(couponRepository, policy);

        // When
        Coupon issuedCoupon = service.issue(userId, discount);

        // Then
        assertEquals(userId, issuedCoupon.getUserId());
        assertEquals(discount, issuedCoupon.getDiscountAmount());
        assertFalse(issuedCoupon.isUsed());
        verify(couponRepository).save(any(Coupon.class));
    }
}
