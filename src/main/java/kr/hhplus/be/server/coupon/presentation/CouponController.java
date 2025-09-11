package kr.hhplus.be.server.coupon.presentation;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.common.CommonResponse;
import kr.hhplus.be.server.common.CommonResultCode;
import kr.hhplus.be.server.coupon.infrastructure.CouponEventProducer;
import kr.hhplus.be.server.coupon.presentation.dto.event.CouponRequestEvent;
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
    private final CouponEventProducer couponEventProducer;

    @PostMapping("/issue/{userId}")
    public ResponseEntity<CommonResponse> issueCoupon(
            @PathVariable Long userId,
            @RequestParam String code) {

        log.info("Kafka 기반 쿠폰 발급 요청: userId={}, code={}", userId, code);

        try {
            // 기본 검증 (쿠폰 정책 존재 여부만 확인)
            couponService.validateCouponPolicy(code);

            // Kafka로 발급 요청 이벤트 발행
            CouponRequestEvent requestEvent = CouponRequestEvent.create(userId, code);
            couponEventProducer.publishCouponRequest(requestEvent);

            // 즉시 응답 반환 (비동기 처리)
            Map<String, Object> response = Map.of(
                    "message", "쿠폰 발급 요청이 접수되었습니다. 선착순으로 처리됩니다.",
                    "userId", userId,
                    "code", code,
                    "requestId", requestEvent.getRequestId(),
                    "status", "REQUESTED"
            );

            return ResponseEntity.accepted()
                    .body(CommonResponse.of(CommonResultCode.ISSUE_COUPON_SUCCESS, response));

        } catch (Exception e) {
            log.error("쿠폰 발급 요청 실패: userId={}, code={}, error={}", userId, code, e.getMessage());

            return ResponseEntity.badRequest()
                    .body(CommonResponse.of(CommonResultCode.COUPON_SOLD_OUT, e.getMessage()));
        }
    }

    // 비동기 쿠폰 발급 상태 조회
    @GetMapping("/issue-status/{userId}")
    public ResponseEntity<CommonResponse> getAsyncIssuanceStatus(
            @PathVariable Long userId,
            @RequestParam String code
    ) {
        CouponService.CouponIssuanceStatus status = couponService.getAsyncIssuanceStatus(userId, code);

        Map<String, Object> response = Map.of(
                "userId", userId,
                "code", code,
                "status", status.name(),
                "description", status.getDescription(),
                "queuePosition", status.getQueuePosition() != null ? status.getQueuePosition() : 0
        );

        return ResponseEntity.ok(
                CommonResponse.of(CommonResultCode.GET_COUPON_SUCCESS, response)
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
