package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@Tag(name = "Order API", description = "주문 및 결제 API")
public class OrderController {

    @Operation(summary = "주문 및 결제", description = "상품 ID 및 수량 목록을 기반으로 주문 및 결제를 수행합니다.")
    @PostMapping
    public ResponseEntity<OrderResponse> order(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(new OrderResponse("주문이 성공적으로 완료되었습니다.", 10000L));
    }
}
