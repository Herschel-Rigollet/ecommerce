package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CouponRollbackServiceTest {
    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponRollbackService couponRollbackService;

    @Test
    @DisplayName("결제 실패 시 쿠폰이 정상 복구된다")
    void rollbackCoupon_success() {
        // Given
        Long couponId = 10L;
        Coupon coupon = new Coupon(1L, couponId, 1000, true);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // When
        couponRollbackService.rollback(couponId);

        // Then
        assertFalse(coupon.isUsed()); // 사용 상태가 false로 복구
        verify(couponRepository).save(coupon);
    }
}
