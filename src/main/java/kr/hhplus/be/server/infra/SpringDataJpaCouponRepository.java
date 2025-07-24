package kr.hhplus.be.server.infra;

public interface SpringDataJpaCouponRepository {
    List<CouponEntity> findByUserId(long userId);
    boolean existsByUserId(long userId);
}
