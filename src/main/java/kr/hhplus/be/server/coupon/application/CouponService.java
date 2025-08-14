package kr.hhplus.be.server.coupon.application;

import jakarta.persistence.OptimisticLockException;
import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import kr.hhplus.be.server.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;


     // 선착순 쿠폰 발급 (동시성 제어 적용)
    @DistributedLock(
            key = "'coupon:issue:' + #code",
            waitTime = 3L,
            leaseTime = 10L,
            failMessage = "쿠폰 발급이 지연되고 있습니다. 잠시 후 다시 시도해주세요."
    )
    public Coupon issueCoupon(Long userId, String code) {
        log.info("쿠폰 발급 시작: userId={}, code={}, thread={}",
                userId, code, Thread.currentThread().getName());

        // 분산락 획득 + 트랜잭션 시작

        // 1. 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다: " + code));

        // 2. 현재 발급된 쿠폰 수 확인
        long issuedCount = couponRepository.countByCode(code);
        log.info("쿠폰 발급 현황 확인: code={}, issuedCount={}, maxCount={}, thread={}",
                code, issuedCount, policy.getMaxCount(), Thread.currentThread().getName());

        if (issuedCount >= policy.getMaxCount()) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다. (발급완료: " + issuedCount + "/" + policy.getMaxCount() + ")");
        }

        // 3. 쿠폰 생성 및 발급
        Coupon coupon = Coupon.builder()
                .userId(userId)
                .code(policy.getCode())
                .discountRate(policy.getDiscountRate())
                .used(false)
                .issuedAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        log.info("쿠폰 발급 완료: couponId={}, userId={}, code={}, thread={}",
                savedCoupon.getCouponId(), userId, code, Thread.currentThread().getName());

        return savedCoupon;

        // 트랜잭션 커밋 -> 분산락 해제
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

    // 낙관적 락 기반 쿠폰 사용 메소드
    @Transactional
    public void useCouponOptimistic(Long couponId, Long userId) {
        try {
            Coupon coupon = couponRepository.findByCouponId(couponId)
                    .orElseThrow(() -> new NoSuchElementException("쿠폰을 찾을 수 없습니다: " + couponId));

            // 쿠폰 유효성 검증
            validateCoupon(coupon, userId);

            // 쿠폰 사용 처리 (낙관적 락 적용)
            coupon.use(); // version이 자동으로 체크됨

            couponRepository.save(coupon);

        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("쿠폰이 이미 사용되었습니다. 다시 시도해주세요.");
        }
    }

    // 쿠폰 사용과 할인 적용을 함께 처리하는 메소드
    @Transactional
    public int useCouponAndCalculateDiscount(Long couponId, Long userId, int totalAmount) {
        try {
            Coupon coupon = couponRepository.findByCouponId(couponId)
                    .orElseThrow(() -> new NoSuchElementException("쿠폰을 찾을 수 없습니다: " + couponId));

            // 쿠폰 유효성 검증
            validateCoupon(coupon, userId);

            // 할인 금액 계산
            int discountedAmount = coupon.calculateDiscountedAmount(totalAmount);

            // 쿠폰 사용 처리 (낙관적 락 적용)
            coupon.use();
            couponRepository.save(coupon);

            return discountedAmount;

        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("쿠폰이 이미 사용되었습니다. 다시 시도해주세요.");
        }
    }
}