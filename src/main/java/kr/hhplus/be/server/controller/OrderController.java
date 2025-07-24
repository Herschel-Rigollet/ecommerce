package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import kr.hhplus.be.server.mapper.OrderMapper;
import kr.hhplus.be.server.service.PlaceOrderUseCase;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @PostMapping
    @Operation(summary = "주문/결제", description = "사용자 ID, 상품 ID, 수량을 입력받아 주문을 처리하고 결제합니다.")
    public OrderResponse placeOrder(@RequestBody OrderRequest request) {
        return OrderMapper.toDto(
                placeOrderUseCase.placeOrder(
                        request.getUserId(),
                        request.getProductId(),
                        request.getQuantity()
                )
        );
    }
}
