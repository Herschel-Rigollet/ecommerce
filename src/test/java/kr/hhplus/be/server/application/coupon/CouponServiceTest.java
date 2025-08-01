package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Test
    void 최대_개수까지만_쿠폰_발급() {
        // Given
        Long userId = 1L;
        String code = "WELCOME10";

        CouponPolicy policy = new CouponPolicy(code, 10, 2); // 최대 2장 발급 가능

        // 1번째 호출 → 발급 수량 0
        when(couponPolicyRepository.findByCodeForUpdate(code))
                .thenReturn(Optional.of(policy));
        when(couponRepository.countByCode(code)).thenReturn(0L);

        Coupon firstCoupon = new Coupon(userId, code, 10);
        when(couponRepository.save(any(Coupon.class))).thenReturn(firstCoupon);

        Coupon result1 = couponService.issueCoupon(userId, code);
        assertNotNull(result1);

        // 2번째 호출 → 발급 수량 1
        when(couponRepository.countByCode(code)).thenReturn(1L);

        Coupon secondCoupon = new Coupon(userId, code, 10);
        when(couponRepository.save(any(Coupon.class))).thenReturn(secondCoupon);

        Coupon result2 = couponService.issueCoupon(userId, code);
        assertNotNull(result2);

        // 3번째 호출 → 발급 수량 2 → 예외 발생
        when(couponRepository.countByCode(code)).thenReturn(2L);

        assertThrows(IllegalStateException.class,
                () -> couponService.issueCoupon(userId, code));
    }
}
