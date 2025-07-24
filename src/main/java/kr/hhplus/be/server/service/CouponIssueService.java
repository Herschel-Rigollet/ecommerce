package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponIssuePolicy;
import kr.hhplus.be.server.domain.CouponRepository;

public class CouponIssueService {
    private final CouponRepository couponRepository;
    private final CouponIssuePolicy issuePolicy;

    public CouponIssueService(CouponRepository couponRepository, CouponIssuePolicy issuePolicy) {
        this.couponRepository = couponRepository;
        this.issuePolicy = issuePolicy;
    }

    public Coupon issue(long userId, int discountAmount) {
        issuePolicy.increaseIssued(); // 수량 초과 검사 포함
        Coupon coupon = new Coupon(userId, discountAmount);
        couponRepository.save(coupon);
        return coupon;
    }
}
