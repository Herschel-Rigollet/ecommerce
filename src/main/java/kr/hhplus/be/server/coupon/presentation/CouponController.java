package kr.hhplus.be.server.coupon.presentation;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.common.CommonResponse;
import kr.hhplus.be.server.common.CommonResultCode;
import kr.hhplus.be.server.coupon.presentation.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
@Slf4j
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

    // 비동기 선착순 쿠폰 발급
    @PostMapping("/issue-async/{userId}")
    public ResponseEntity<CommonResponse> issueCouponAsync(
            @PathVariable Long userId,
            @RequestParam String code
    ) {
        log.info("비동기 쿠폰 발급 요청: userId={}, code={}", userId, code);

        try {
            // ★ 비동기 발급 시작 (즉시 반환) ★
            CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, code);

            // 즉시 응답 반환 (HTTP 202 Accepted)
            Map<String, Object> response = Map.of(
                    "message", "쿠폰 발급이 진행 중입니다.",
                    "userId", userId,
                    "code", code,
                    "status", "PROCESSING"
            );

            log.info("비동기 쿠폰 발급 요청 접수: userId={}, code={}", userId, code);

            return ResponseEntity.accepted() // HTTP 202
                    .body(CommonResponse.of(CommonResultCode.ISSUE_COUPON_SUCCESS, response));

        } catch (Exception e) {
            log.error("비동기 쿠폰 발급 요청 실패: userId={}, code={}, error={}", userId, code, e.getMessage());

            return ResponseEntity.badRequest()
                    .body(CommonResponse.of(CommonResultCode.COUPON_SOLD_OUT, e.getMessage()));
        }
    }

    // 비동기 쿠폰 발급 결과 대기 API
    @PostMapping("/issue-async-wait/{userId}")
    public ResponseEntity<CommonResponse> issueCouponAsyncWithWait(
            @PathVariable Long userId,
            @RequestParam String code
    ) {
        log.info("비동기 쿠폰 발급 (대기) 요청: userId={}, code={}", userId, code);

        try {
            // 비동기 발급 시작
            CompletableFuture<Coupon> future = couponService.issueCouponAsync(userId, code);

            // ★ 최대 5초간 결과 대기 ★
            Coupon coupon = future.get(5, TimeUnit.SECONDS);

            log.info("비동기 쿠폰 발급 완료: userId={}, couponId={}", userId, coupon.getCouponId());

            return ResponseEntity.ok(
                    CommonResponse.of(CommonResultCode.ISSUE_COUPON_SUCCESS, CouponResponse.from(coupon))
            );

        } catch (java.util.concurrent.TimeoutException e) {
            // 5초 타임아웃 시
            log.warn("비동기 쿠폰 발급 타임아웃: userId={}, code={}", userId, code);

            Map<String, Object> response = Map.of(
                    "message", "쿠폰 발급이 진행 중입니다. 잠시 후 다시 확인해주세요.",
                    "userId", userId,
                    "code", code,
                    "status", "TIMEOUT"
            );

            return ResponseEntity.accepted()
                    .body(CommonResponse.of(CommonResultCode.ISSUE_COUPON_SUCCESS, response));

        } catch (Exception e) {
            log.error("비동기 쿠폰 발급 실패: userId={}, code={}, error={}", userId, code, e.getMessage());

            return ResponseEntity.badRequest()
                    .body(CommonResponse.of(CommonResultCode.COUPON_SOLD_OUT, e.getMessage()));
        }
    }
}
