package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import kr.hhplus.be.server.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;


     // 선착순 쿠폰 발급 (동시성 제어 적용)
    @Transactional
    public Coupon issueCoupon(Long userId, String code) {
        // 1. 쿠폰 정책을 비관적 락으로 조회 (동시성 제어)
        CouponPolicy policy = couponPolicyRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다: " + code));

        // 2. 현재 발급된 쿠폰 수 확인
        long issuedCount = couponRepository.countByCode(code);
        if (issuedCount >= policy.getMaxCount()) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        // 3. 쿠폰 발급
        Coupon coupon = Coupon.builder()
                .userId(userId)
                .code(policy.getCode())
                .discountRate(policy.getDiscountRate())
                .used(false)
                .issuedAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(30)) // 30일 후 만료
                .build();

        return couponRepository.save(coupon);
    }

    // 사용자 보유 쿠폰 목록 조회
    @Transactional(readOnly = true)
    public List<Coupon> getUserCoupons(Long userId) {
        return couponRepository.findByUserId(userId);
    }

    // 쿠폰 상세 조회
    @Transactional(readOnly = true)
    public Coupon getCouponById(Long couponId) {
        return couponRepository.findByCouponId(couponId)
                .orElseThrow(() -> new NoSuchElementException("쿠폰을 찾을 수 없습니다: " + couponId));
    }

    // 쿠폰 저장
    @Transactional
    public void saveCoupon(Coupon coupon) {
        couponRepository.save(coupon);
    }

    // 쿠폰 유효성 검증
    public void validateCoupon(Coupon coupon, Long userId) {
        if (!coupon.getUserId().equals(userId)) {
            throw new IllegalStateException("해당 쿠폰은 이 사용자 소유가 아닙니다.");
        }
        if (coupon.isUsed()) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        if (coupon.isExpired()) {
            throw new IllegalStateException("만료된 쿠폰입니다.");
        }
    }
}