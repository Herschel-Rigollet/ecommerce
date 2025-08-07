package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.application.OrderService;
import kr.hhplus.be.server.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

//    @Mock
//    private OrderRepository orderRepository;
//
//    @InjectMocks
//    private OrderService orderService;
//
//    @Test
//    void 주문을_생성하고_저장할_수_있다() {
//        // Given
//        Long userId = 1L;
//        OrderItem orderItem = new OrderItem(10L, 2, 5000); // 총 금액: 10,000
//        List<OrderItem> orderItems = List.of(orderItem);
//
//        Order expectedOrder = new Order(userId, orderItems);
//
//        when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);
//
//        // When
//        Order result = orderService.saveOrder(userId, orderItems);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(userId, result.getUserId());
//        assertEquals(10_000, result.getTotalAmount());
//        assertEquals(1, result.getItems().size());
//        assertEquals(10L, result.getItems().get(0).getProductId());
//        assertEquals(2, result.getItems().get(0).getQuantity());
//        assertEquals(5000, result.getItems().get(0).getUnitPrice());
//
//        verify(orderRepository, times(1)).save(any(Order.class));
//    }
}
