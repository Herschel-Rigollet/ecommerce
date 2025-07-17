package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.BalanceRequest;
import kr.hhplus.be.server.dto.BalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/balance")
@Tag(name = "Balance API", description = "잔액 충전 및 조회 API")
public class BalanceController {

    @Operation(summary = "잔액 충전", description = "사용자 식별자와 금액을 입력받아 잔액을 충전합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "충전 성공",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    })
    @PostMapping("/charge")
    public ResponseEntity<BalanceResponse> charge(@RequestBody BalanceRequest request) {
        return ResponseEntity.ok(new BalanceResponse(request.getUserId(), request.getAmount()));
    }

    @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(new BalanceResponse(userId, 10000L));
    }
}
