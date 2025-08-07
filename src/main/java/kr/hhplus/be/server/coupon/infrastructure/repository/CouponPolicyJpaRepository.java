package kr.hhplus.be.server.coupon.infrastructure.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 선착순 쿠폰: 비관적 락
public interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicy, Long> {
    // 코드로 쿠폰 정책 조회
    Optional<CouponPolicy> findByCode(String code);

    // 동시성 제어를 위한 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cp FROM CouponPolicy cp WHERE cp.code = :code")
    Optional<CouponPolicy> findByCodeForUpdate(@Param("code") String code);
}
