package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.CouponResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/coupons")
@Tag(name = "Coupon API", description = "선착순 쿠폰 발급 및 보유 쿠폰 조회 API")
public class CouponController {

    @Operation(summary = "쿠폰 발급", description = "선착순으로 사용자에게 쿠폰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰 발급 성공",
                    content = @Content(schema = @Schema(implementation = CouponResponse.class))),
            @ApiResponse(responseCode = "409", description = "쿠폰 모두 소진됨",
                    content = @Content(schema = @Schema(example = "{\"message\": \"쿠폰이 모두 소진되었습니다.\"}")))
    })
    @PostMapping("/issue/{userId}")
    public ResponseEntity<CouponResponse> issueCoupon(@PathVariable Long userId) {
        Long couponId = 0L;
        return ResponseEntity.ok(new CouponResponse(userId, couponId, 10.0, false, LocalDateTime.of(2025, 05, 12, 0, 0, 0), LocalDateTime.of(2025, 10, 12, 0, 0, 0)));
    }

    @Operation(summary = "보유 쿠폰 목록 조회", description = "사용자가 보유한 쿠폰 목록을 반환합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<List<CouponResponse>> getUserCoupons(@PathVariable Long userId) {
        Long couponId = 0L;
        return ResponseEntity.ok(List.of(
                new CouponResponse(userId, couponId, 10.0, false, LocalDateTime.of(2025, 05, 12, 0, 0, 0), LocalDateTime.of(2025, 10, 12, 0, 0, 0))
        ));
    }
}