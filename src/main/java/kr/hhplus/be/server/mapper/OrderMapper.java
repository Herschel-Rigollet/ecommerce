package kr.hhplus.be.server.mapper;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.dto.OrderResponse;

public class OrderMapper {
    public static OrderResponse toDto(Order order) {
        return new OrderResponse(
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount()
        );
    }
}
