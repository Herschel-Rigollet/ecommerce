package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductRepository;
import kr.hhplus.be.server.application.user.UserRepository;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.presentation.order.request.OrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void 주문에_성공하면_재고와_포인트가_차감되고_주문이_저장된다() {
        // Given
        Long userId = 1L;
        Long productId = 100L;

        User user = new User(userId);
        user.charge(20_000);

        Product product = new Product(productId, "상품A", 5_000, 10);

        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(productId, 2)));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 저장된 Order 객체 그대로 반환

        // When
        Order result = orderService.placeOrder(request);

        // Then
        assertEquals(10_000, result.getTotalAmount());
        assertEquals(10_000, user.getPoint()); // 20,000 - 10,000
        assertEquals(8, product.getStock()); // 10 - 2
        assertEquals(1, result.getItems().size());
        assertEquals(productId, result.getItems().get(0).getProductId());
    }

    @Test
    void 유저가_존재하지_않으면_예외가_발생한다() {
        // Given
        Long userId = 999L;
        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(1L, 1)));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(request));
    }

    @Test
    void 상품이_존재하지_않으면_예외가_발생한다() {
        // Given
        Long userId = 1L;
        Long productId = 999L;

        User user = new User(userId);
        user.charge(10_000);

        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(productId, 1)));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(request));
    }

    @Test
    void 재고가_부족하면_예외가_발생한다() {
        // Given
        Long userId = 1L;
        Long productId = 10L;

        User user = new User(userId);
        user.charge(100_000);

        Product product = new Product(productId, "상품B", 3_000, 1); // 재고 1

        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(productId, 5))); // 수량 5

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.placeOrder(request));
    }

    @Test
    void 잔액이_부족하면_예외가_발생한다() {
        // Given
        Long userId = 1L;
        Long productId = 20L;

        User user = new User(userId);
        user.charge(1_000); // 포인트 부족

        Product product = new Product(productId, "상품C", 5_000, 10);

        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(productId, 1))); // 총액 5,000

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.placeOrder(request));
    }
}