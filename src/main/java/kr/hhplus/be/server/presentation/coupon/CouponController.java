package kr.hhplus.be.server.presentation.coupon;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.presentation.common.CommonResponse;
import kr.hhplus.be.server.presentation.common.CommonResultCode;
import kr.hhplus.be.server.presentation.coupon.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;

    // 선착순 쿠폰 발급
    @PostMapping("/issue/{userId}")
    public ResponseEntity<CommonResponse> issueCoupon(
            @PathVariable Long userId,
            @RequestParam String code // 쿠폰 코드
    ) {
        Coupon coupon = couponService.issueCoupon(userId, code);
        return ResponseEntity.ok(
                CommonResponse.of(CommonResultCode.ISSUE_COUPON_SUCCESS, CouponResponse.from(coupon))
        );
    }

    // 보유 쿠폰 조회
    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse> getCoupons(@PathVariable Long userId) {
        List<CouponResponse> coupons = couponService.getUserCoupons(userId)
                .stream()
                .map(CouponResponse::from)
                .toList();

        return ResponseEntity.ok(
                CommonResponse.of(CommonResultCode.GET_COUPON_SUCCESS, coupons)
        );
    }
}
