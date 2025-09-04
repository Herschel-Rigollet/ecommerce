package kr.hhplus.be.server.coupon.domain.repository;

import kr.hhplus.be.server.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    long countByCode(String code); // 현재 발급 수
    List<Coupon> findByUserId(Long userId); // 유저의 보유 쿠폰 목록

    Coupon save(Coupon coupon); // 발급

    Optional<Coupon> findByCouponId(Long couponId); // 주문 시 조회용

    boolean existsByUserIdAndCode(Long userId, String couponCode);
}
