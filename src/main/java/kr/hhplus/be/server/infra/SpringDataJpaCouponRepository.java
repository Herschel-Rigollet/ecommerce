package kr.hhplus.be.server.infra;

import kr.hhplus.be.server.entity.CouponEntity;

import java.util.List;

public interface SpringDataJpaCouponRepository {
    List<CouponEntity> findByUserId(long userId);
    boolean existsByUserId(long userId);
}
