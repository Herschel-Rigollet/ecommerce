package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.BalanceChargeRequest;
import kr.hhplus.be.server.dto.BalanceResponse;
import kr.hhplus.be.server.dto.BalanceUseRequest;
import kr.hhplus.be.server.mapper.BalanceMapper;
import kr.hhplus.be.server.service.BalanceChargeService;
import kr.hhplus.be.server.service.BalanceQueryService;
import kr.hhplus.be.server.service.BalanceUseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/balance")
public class BalanceController {
    private final BalanceChargeService chargeService;
    private final BalanceUseService useService;
    private final BalanceQueryService queryService;

    public BalanceController(
            BalanceChargeService chargeService,
            BalanceUseService useService,
            BalanceQueryService queryService
    ) {
        this.chargeService = chargeService;
        this.useService = useService;
        this.queryService = queryService;
    }

    @GetMapping("/{userId}")
    public BalanceResponse getBalance(@PathVariable Long userId) {
        return BalanceMapper.toDto(queryService.getBalance(userId));
    }

    @PatchMapping("/{userId}/charge")
    public void charge(@PathVariable Long userId, @RequestBody BalanceChargeRequest request) {
        chargeService.charge(userId, request.getAmount());
    }

    @PatchMapping("/{userId}/use")
    public void use(@PathVariable Long userId, @RequestBody BalanceUseRequest request) {
        useService.use(userId, request.getAmount());
    }
}
