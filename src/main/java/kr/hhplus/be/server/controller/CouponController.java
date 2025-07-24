package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponRepository;
import kr.hhplus.be.server.dto.CouponIssueRequest;
import kr.hhplus.be.server.dto.CouponResponse;
import kr.hhplus.be.server.service.CouponIssueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupon")
public class CouponController {
    private final CouponIssueService couponIssueService;
    private final CouponRepository couponRepository;

    public CouponController(CouponIssueService couponIssueService, CouponRepository couponRepository) {
        this.couponIssueService = couponIssueService;
        this.couponRepository = couponRepository;
    }

    // 선착순 쿠폰 발급 API
    @PostMapping("/issue/{userId}")
    public CouponResponse issue(
            @PathVariable long userId,
            @RequestBody CouponIssueRequest request
    ) {
        Coupon issued = couponIssueService.issue(userId, request.getDiscountAmount());
        return CouponResponse.from(issued);
    }

    // 사용자 보유 쿠폰 목록 조회 API
    @GetMapping("/list/{userId}")
    public List<CouponResponse> list(@PathVariable long userId) {
        return couponRepository.findByUserId(userId)
                .stream()
                .map(CouponResponse::from)
                .toList();
    }
}
