package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceRepository;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ProductRepository;
import kr.hhplus.be.server.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlaceOrderUseCaseTest {
    private ProductRepository productRepository;
    private BalanceRepository balanceRepository;
    private OrderRepository orderRepository;
    private DataPlatformSender sender;
    private PlaceOrderUseCase placeOrderUseCase;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        balanceRepository = mock(BalanceRepository.class);
        orderRepository = mock(OrderRepository.class);
        sender = mock(DataPlatformSender.class);
        placeOrderUseCase = new PlaceOrderUseCase(productRepository, balanceRepository, orderRepository, sender);
    }

    @Test
    void 정상적으로_주문을_수행할_수_있다() {
        // Given
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 2;

        Product product = new Product(productId, "키보드", 5000, 10);
        User balance = new User(userId, 20000);

        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));
        when(balanceRepository.findByUserId(userId)).thenReturn(balance);

        // When
        Order result = placeOrderUseCase.placeOrder(userId, productId, quantity);

        // Then
        assertEquals(userId, result.getUserId());
        assertEquals(10000, result.getTotalAmount());
        verify(orderRepository).save(any(Order.class));
        verify(sender).send(any(Order.class));
    }

    @Test
    void 재고가_부족하면_예외가_발생한다() {
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 100; // 재고보다 많음

        Product product = new Product(productId, "마우스", 3000, 5);
        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));

        assertThrows(IllegalStateException.class, () ->
                placeOrderUseCase.placeOrder(userId, productId, quantity));
    }

    @Test
    void 잔액이_부족하면_예외가_발생한다() {
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 2;

        Product product = new Product(productId, "모니터", 10000, 5);
        User balance = new User(userId, 5000);

        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));
        when(balanceRepository.findByUserId(userId)).thenReturn(balance);

        assertThrows(IllegalArgumentException.class, () ->
                placeOrderUseCase.placeOrder(userId, productId, quantity));
    }
}
