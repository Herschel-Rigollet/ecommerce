package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public Coupon issueCoupon(Long userId, String code) {
        // 쿠폰 정책을 비관적 락으로 조회 (동시성 제어)
        CouponPolicy policy = couponPolicyRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다."));

        // 현재 발급된 쿠폰 수 확인
        long issuedCount = couponRepository.countByCode(code);
        if (issuedCount >= policy.getMaxCount()) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        // 쿠폰 발급
        Coupon coupon = new Coupon(userId, policy.getCode(), policy.getDiscountRate());
        return couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getUserCoupons(Long userId) {
        return couponRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Coupon getCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new NoSuchElementException("쿠폰을 찾을 수 없습니다."));
    }

    @Transactional
    public void saveCoupon(Coupon coupon) {
        couponRepository.save(coupon);
    }
}