package kr.hhplus.be.server.order.presentation;

import kr.hhplus.be.server.order.application.OrderFacade;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.common.CommonResponse;
import kr.hhplus.be.server.common.CommonResultCode;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.presentation.dto.request.OrderRequest;
import kr.hhplus.be.server.order.presentation.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<CommonResponse> placeOrder(@RequestBody OrderRequest request) {
        OrderResponse orderResponse = orderFacade.placeOrder(request);

        return ResponseEntity.ok(
                CommonResponse.of(CommonResultCode.ORDER_SUCCESS, orderResponse)
        );
    }
}