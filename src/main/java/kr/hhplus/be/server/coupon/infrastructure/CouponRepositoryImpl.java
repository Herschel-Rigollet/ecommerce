package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import kr.hhplus.be.server.coupon.infrastructure.repository.CouponJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository couponJpaRepository;

    @Override
    public long countByCode(String code) {
        return couponJpaRepository.countByCode(code);
    }

    @Override
    public List<Coupon> findByUserId(Long userId) {
        return couponJpaRepository.findByUserId(userId);
    }

    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findByCouponId(Long couponId) {
        return couponJpaRepository.findById(couponId);
    }

    @Override
    public boolean existsByUserIdAndCode(Long userId, String couponCode) {
        return couponJpaRepository.existsByUserIdAndCode(userId, couponCode);
    }
}
