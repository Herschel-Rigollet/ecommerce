package kr.hhplus.be.server.domain;

import java.util.List;

public interface CouponRepository {
    void save(Coupon coupon);
    List<Coupon> findByUserId(long userId);
    boolean existsByUserId(long userId); // 중복 발급 방지용
}
