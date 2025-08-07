package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {
        // Given
        Long userId = 1L;
        String couponCode = "WELCOME10";
        CouponPolicy policy = createCouponPolicy(1L, couponCode, 10, 100); // 최대 100개

        given(couponPolicyRepository.findByCodeForUpdate(couponCode))
                .willReturn(Optional.of(policy));
        given(couponRepository.countByCode(couponCode)).willReturn(50L);

        Coupon expectedCoupon = Coupon.builder()
                .couponId(1L)
                .userId(userId)
                .code(couponCode)
                .discountRate(10)
                .used(false)
                .issuedAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();

        given(couponRepository.save(any(Coupon.class))).willReturn(expectedCoupon);

        // When
        Coupon result = couponService.issueCoupon(userId, couponCode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCouponId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCode()).isEqualTo(couponCode);
        assertThat(result.getDiscountRate()).isEqualTo(10);
        assertThat(result.isUsed()).isFalse();

        verify(couponPolicyRepository).findByCodeForUpdate(couponCode);
        verify(couponRepository).countByCode(couponCode);
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 코드 발급 시 예외 발생")
    void issueCoupon_InvalidCode_ThrowsException() {
        // Given
        Long userId = 1L;
        String invalidCode = "INVALID";

        given(couponPolicyRepository.findByCodeForUpdate(invalidCode))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, invalidCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 쿠폰 코드입니다: " + invalidCode);

        verify(couponPolicyRepository).findByCodeForUpdate(invalidCode);
        verifyNoInteractions(couponRepository); // countByCode도 호출되지 않아야 함
    }

    @Test
    @DisplayName("쿠폰 발급 한도 초과 시 예외 발생")
    void issueCoupon_ExceedsLimit_ThrowsException() {
        // Given
        Long userId = 1L;
        String couponCode = "WELCOME10";
        CouponPolicy policy = createCouponPolicy(1L, couponCode, 10, 100); // 최대 100개

        given(couponPolicyRepository.findByCodeForUpdate(couponCode))
                .willReturn(Optional.of(policy));
        given(couponRepository.countByCode(couponCode)).willReturn(100L); // 이미 100개 발급됨 (= 한도)

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponCode))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰이 모두 소진되었습니다.");

        verify(couponPolicyRepository).findByCodeForUpdate(couponCode);
        verify(couponRepository).countByCode(couponCode);
        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 한도 초과 - 경계값 테스트")
    void issueCoupon_ExactlyAtLimit_ThrowsException() {
        // Given
        Long userId = 1L;
        String couponCode = "WELCOME10";
        CouponPolicy policy = createCouponPolicy(1L, couponCode, 10, 50); // 최대 50개

        given(couponPolicyRepository.findByCodeForUpdate(couponCode))
                .willReturn(Optional.of(policy));
        given(couponRepository.countByCode(couponCode)).willReturn(50L); // 정확히 50개 (= 한도)

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponCode))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰이 모두 소진되었습니다.");
    }

    @Test
    @DisplayName("쿠폰 발급 가능 - 경계값 테스트")
    void issueCoupon_OneBelowLimit_Success() {
        // Given
        Long userId = 1L;
        String couponCode = "WELCOME10";
        CouponPolicy policy = createCouponPolicy(1L, couponCode, 10, 50); // 최대 50개

        given(couponPolicyRepository.findByCodeForUpdate(couponCode))
                .willReturn(Optional.of(policy));
        given(couponRepository.countByCode(couponCode)).willReturn(49L); // 49개 발급됨 (< 50)

        Coupon expectedCoupon = Coupon.builder()
                .couponId(1L)
                .userId(userId)
                .code(couponCode)
                .discountRate(10)
                .used(false)
                .build();

        given(couponRepository.save(any(Coupon.class))).willReturn(expectedCoupon);

        // When
        Coupon result = couponService.issueCoupon(userId, couponCode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCode()).isEqualTo(couponCode);

        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 성공")
    void getUserCoupons_Success() {
        // Given
        Long userId = 1L;
        Coupon coupon1 = createCoupon(1L, userId, "WELCOME10", 10, false);
        Coupon coupon2 = createCoupon(2L, userId, "SUMMER20", 20, false);

        given(couponRepository.findByUserId(userId))
                .willReturn(java.util.Arrays.asList(coupon1, coupon2));

        // When
        var result = couponService.getUserCoupons(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCouponId()).isEqualTo(1L);
        assertThat(result.get(1).getCouponId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("쿠폰 상세 조회 성공")
    void getCouponById_Success() {
        // Given
        Long couponId = 1L;
        Coupon coupon = createCoupon(couponId, 1L, "WELCOME10", 10, false);

        given(couponRepository.findByCouponId(couponId))
                .willReturn(Optional.of(coupon));

        // When
        Coupon result = couponService.getCouponById(couponId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCouponId()).isEqualTo(couponId);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 조회 시 예외 발생")
    void getCouponById_NotFound_ThrowsException() {
        // Given
        Long couponId = 999L;
        given(couponRepository.findByCouponId(couponId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.getCouponById(couponId))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("쿠폰을 찾을 수 없습니다: " + couponId);
    }

    // Helper methods
    private CouponPolicy createCouponPolicy(Long policyId, String code, int discountRate, int maxCount) {
        try {
            CouponPolicy policy = new CouponPolicy();

            // Reflection을 사용하여 private 필드 설정
            java.lang.reflect.Field idField = CouponPolicy.class.getDeclaredField("policyId");
            idField.setAccessible(true);
            idField.set(policy, policyId);

            java.lang.reflect.Field codeField = CouponPolicy.class.getDeclaredField("code");
            codeField.setAccessible(true);
            codeField.set(policy, code);

            java.lang.reflect.Field discountRateField = CouponPolicy.class.getDeclaredField("discountRate");
            discountRateField.setAccessible(true);
            discountRateField.set(policy, discountRate);

            java.lang.reflect.Field maxCountField = CouponPolicy.class.getDeclaredField("maxCount");
            maxCountField.setAccessible(true);
            maxCountField.set(policy, maxCount);

            return policy;
        } catch (Exception e) {
            throw new RuntimeException("CouponPolicy 객체 생성 실패", e);
        }
    }

    private Coupon createCoupon(Long couponId, Long userId, String code, int discountRate, boolean used) {
        return Coupon.builder()
                .couponId(couponId)
                .userId(userId)
                .code(code)
                .discountRate(discountRate)
                .used(used)
                .issuedAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();
    }
}
