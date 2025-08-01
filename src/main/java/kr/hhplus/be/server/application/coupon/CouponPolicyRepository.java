package kr.hhplus.be.server.application.coupon;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) // SELECT ... FOR UPDATE
    @Query("SELECT p FROM CouponPolicy p WHERE p.code = :code")
    Optional<CouponPolicy> findByCodeForUpdate(@Param("code") String code);

    Optional<CouponPolicy> findByCode(String code);
}
