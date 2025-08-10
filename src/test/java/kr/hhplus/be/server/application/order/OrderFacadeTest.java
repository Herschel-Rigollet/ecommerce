package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.order.application.OrderFacade;
import kr.hhplus.be.server.order.application.OrderService;
import kr.hhplus.be.server.order.application.StockRollbackService;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.order.presentation.dto.request.OrderRequest;
import kr.hhplus.be.server.order.presentation.dto.response.OrderResponse;
import kr.hhplus.be.server.order.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private StockRollbackService stockRollbackService;

    @Mock
    private CouponService couponService;

    @Test
    @DisplayName("주문 성공")
    void placeOrder_Success() {
        // Given
        Long userId = 1L;
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(1L, 2);
        OrderRequest orderRequest = new OrderRequest(userId, Arrays.asList(itemRequest));

        User user = createUserWithId(userId, 50000L);
        Product product = createProduct(1L, "iPhone 15", 20000, 10);
        Order savedOrder = createOrder(1L, userId, 40000);

        given(userService.getPointByUserId(userId)).willReturn(user);
        given(productService.getProductById(1L)).willReturn(product);

        given(orderService.saveOrder(any(Long.class), any(List.class))).willReturn(savedOrder);

        // When
        OrderResponse result = orderFacade.placeOrder(orderRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalAmount()).isEqualTo(40000);

        // 사용자 포인트 차감 확인
        assertThat(user.getPoint()).isEqualTo(10000L); // 50000 - 40000

        // 상품 재고 차감 확인
        assertThat(product.getStock()).isEqualTo(8); // 10 - 2

        // Mock 호출 검증
        verify(orderService).saveOrder(eq(userId), any(List.class));
    }

    @Test
    @DisplayName("주문 성공 - ArgumentCaptor로 정확한 값 검증")
    void placeOrder_Success_WithArgumentCaptor() {
        // Given
        Long userId = 1L;
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(1L, 2);
        OrderRequest orderRequest = new OrderRequest(userId, Arrays.asList(itemRequest));

        User user = createUserWithId(userId, 50000L);
        Product product = createProduct(1L, "iPhone 15", 20000, 10);
        Order savedOrder = createOrder(1L, userId, 40000);

        given(userService.getPointByUserId(userId)).willReturn(user);
        given(productService.getProductById(1L)).willReturn(product);
        given(orderService.saveOrder(any(Long.class), any(List.class))).willReturn(savedOrder);

        // When
        OrderResponse result = orderFacade.placeOrder(orderRequest);

        // Then
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);

        verify(orderService).saveOrder(userIdCaptor.capture(), itemsCaptor.capture());

        // 실제 전달된 값들 검증
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);

        List<OrderItem> capturedItems = itemsCaptor.getValue();
        assertThat(capturedItems).hasSize(1);
        assertThat(capturedItems.get(0).getProductId()).isEqualTo(1L);
        assertThat(capturedItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(capturedItems.get(0).getUnitPrice()).isEqualTo(20000);
        assertThat(capturedItems.get(0).getTotalPrice()).isEqualTo(40000);
    }

    @Test
    @DisplayName("재고 부족 시 주문 실패")
    void placeOrder_InsufficientStock_ThrowsException() {
        // Given
        Long userId = 1L;
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(1L, 15);
        OrderRequest orderRequest = new OrderRequest(userId, Arrays.asList(itemRequest));

        User user = createUserWithId(userId, 50000L);
        Product product = createProduct(1L, "iPhone 15", 20000, 10); // 재고 10개

        given(userService.getPointByUserId(userId)).willReturn(user);
        given(productService.getProductById(1L)).willReturn(product);

        // When & Then
        assertThatThrownBy(() -> orderFacade.placeOrder(orderRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고가 부족한 상품입니다: iPhone 15");

        // orderService.saveOrder는 호출되지 않아야 함
        verify(orderService, never()).saveOrder(any(), any());
    }

    @Test
    @DisplayName("잔액 부족 시 주문 실패 및 재고 복구")
    void placeOrder_InsufficientBalance_RollbackStock() {
        // Given
        Long userId = 1L;
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(1L, 2);
        OrderRequest orderRequest = new OrderRequest(userId, Arrays.asList(itemRequest));

        User user = createUserWithId(userId, 10000L); // 잔액 부족
        Product product = createProduct(1L, "iPhone 15", 20000, 10);

        given(userService.getPointByUserId(userId)).willReturn(user);
        given(productService.getProductById(1L)).willReturn(product);

        // When & Then
        assertThatThrownBy(() -> orderFacade.placeOrder(orderRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액이 부족합니다.");

        // 재고 복구 및 orderService 호출 검증
        verify(stockRollbackService).rollback(any(List.class));
        verify(orderService, never()).saveOrder(any(), any());
    }

    private User createUserWithId(Long userId, long point) {
        User user = new User();

        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("userId");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException("User ID 설정 실패", e);
        }

        // 포인트 충전
        if (point > 0) {
            user.charge(point);
        }

        return user;
    }

    private Product createProduct(Long id, String name, int price, int stock) {
        Product product = new Product();
        try {
            java.lang.reflect.Field idField = Product.class.getDeclaredField("productId");
            idField.setAccessible(true);
            idField.set(product, id);

            java.lang.reflect.Field nameField = Product.class.getDeclaredField("productName");
            nameField.setAccessible(true);
            nameField.set(product, name);

            java.lang.reflect.Field priceField = Product.class.getDeclaredField("price");
            priceField.setAccessible(true);
            priceField.set(product, price);

            java.lang.reflect.Field stockField = Product.class.getDeclaredField("stock");
            stockField.setAccessible(true);
            stockField.set(product, stock);
        } catch (Exception e) {
            throw new RuntimeException("Product 객체 생성 실패", e);
        }
        return product;
    }

    private Order createOrder(Long orderId, Long userId, int totalAmount) {
        List<OrderItem> items = createOrderItemsForAmount(totalAmount);
        Order order = new Order(userId, items);

        try {
            java.lang.reflect.Field idField = Order.class.getDeclaredField("orderId");
            idField.setAccessible(true);
            idField.set(order, orderId);
        } catch (Exception e) {
        }

        return order;
    }

    private List<OrderItem> createOrderItemsForAmount(int totalAmount) {
        OrderItem item = new OrderItem(1L, 1, totalAmount);
        return Arrays.asList(item);
    }

    private Coupon createCoupon(Long couponId, Long userId, String code, int discountRate) {
        return Coupon.builder()
                .couponId(couponId)
                .userId(userId)
                .code(code)
                .discountRate(discountRate)
                .used(false)
                .issuedAt(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();
    }
}