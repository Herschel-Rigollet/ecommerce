package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.presentation.order.request.OrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @Test
    void 주문_정상_성공() {
        // Given
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 2;
        int unitPrice = 500;
        int userPoint = 2000;

        Product product = new Product(productId, "상품A", unitPrice, 10);
        User user = new User(userId, userPoint);
        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(productId, quantity)));

        OrderItem expectedOrderItem = new OrderItem(productId, quantity, unitPrice);
        Order expectedOrder = new Order(userId, List.of(expectedOrderItem));

        given(userService.getPointByUserId(userId)).willReturn(user);
        given(productService.getProductById(productId)).willReturn(product);
        given(orderService.saveOrder(eq(userId), anyList())).willReturn(expectedOrder);

        // When
        Order result = orderFacade.placeOrder(request);

        // Then
        assertEquals(userId, result.getUserId());
        assertEquals(1, result.getItems().size());
        assertEquals(quantity * unitPrice, result.getTotalAmount());
        assertEquals(productId, result.getItems().get(0).getProductId());
    }

    @Test
    void 주문_실패_재고부족() {
        // Given
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 5;
        int stock = 2; // 재고 부족
        int price = 1000;

        User user = new User(userId, 10000);
        Product product = new Product(productId, "상품B", price, stock);
        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(productId, quantity)));

        given(userService.getPointByUserId(userId)).willReturn(user);
        given(productService.getProductById(productId)).willReturn(product);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderFacade.placeOrder(request));
        assertTrue(exception.getMessage().contains("재고가 부족"));
    }

    @Test
    void 주문_실패_포인트부족_재고롤백() {
        // Given
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 3;
        int unitPrice = 1000;
        int stock = 10;
        int userPoint = 1000; // 부족

        Product product = new Product(productId, "상품C", unitPrice, stock);
        User user = new User(userId, userPoint);

        OrderRequest request = new OrderRequest(userId,
                List.of(new OrderRequest.OrderItemRequest(productId, quantity)));

        given(userService.getPointByUserId(userId)).willReturn(user);
        given(productService.getProductById(productId)).willReturn(product);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderFacade.placeOrder(request));

        assertTrue(exception.getMessage().contains("잔액이 부족"));
        verify(stockRollbackService).rollback(anyList());
    }
}

