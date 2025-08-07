package kr.hhplus.be.server.coupon.infrastructure.repository;

import kr.hhplus.be.server.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    // 코드별 발급된 쿠폰 수 카운트
    long countByCode(String code);

    // 사용자별 보유 쿠폰 조회 (모든 쿠폰)
    List<Coupon> findByUserId(Long userId);

    // 사용자별 사용 가능한 쿠폰만 조회
    List<Coupon> findByUserIdAndUsedFalse(Long userId);
}
