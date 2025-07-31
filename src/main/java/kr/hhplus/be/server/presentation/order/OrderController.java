package kr.hhplus.be.server.presentation.order;

import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.presentation.common.CommonResponse;
import kr.hhplus.be.server.presentation.common.CommonResultCode;
import kr.hhplus.be.server.presentation.order.request.OrderRequest;
import kr.hhplus.be.server.presentation.order.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<CommonResponse> placeOrder(@RequestBody OrderRequest request) {
        Order order = orderFacade.placeOrder(request);
        return ResponseEntity.ok(
                CommonResponse.of(CommonResultCode.ORDER_SUCCESS, OrderResponse.from(order))
        );
    }
}