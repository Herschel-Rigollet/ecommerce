package kr.hhplus.be.server.coupon.domain.repository;

import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponPolicyRepository {
    Optional<CouponPolicy> findByCode(String code); // 쿠폰 정책 조회
    Optional<CouponPolicy> findByCodeForUpdate(@Param("code") String code);
}
