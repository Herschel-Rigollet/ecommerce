package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.coupon.application.CouponService;
import kr.hhplus.be.server.order.application.OrderFacade;
import kr.hhplus.be.server.order.application.OrderService;
import kr.hhplus.be.server.order.application.StockRollbackService;
import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.user.application.UserService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

//    @Test
//    void 주문_정상_성공() {
//        // Given
//        Long userId = 1L;
//        Long productId = 100L;
//        int quantity = 2;
//        int unitPrice = 500;
//        int userPoint = 2000;
//
//        Product product = new Product(productId, "상품A", unitPrice, 10);
//        User user = new User(userId, userPoint);
//        OrderRequest request = new OrderRequest(userId,
//                List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
//
//        OrderItem expectedOrderItem = new OrderItem(productId, quantity, unitPrice);
//        Order expectedOrder = new Order(userId, List.of(expectedOrderItem));
//
//        given(userService.getPointByUserId(userId)).willReturn(user);
//        given(productService.getProductById(productId)).willReturn(product);
//        given(orderService.saveOrder(eq(userId), anyList())).willReturn(expectedOrder);
//
//        // When
//        Order result = orderFacade.placeOrder(request);
//
//        // Then
//        assertEquals(userId, result.getUserId());
//        assertEquals(1, result.getItems().size());
//        assertEquals(quantity * unitPrice, result.getTotalAmount());
//        assertEquals(productId, result.getItems().get(0).getProductId());
//    }
//
//    @Test
//    void 주문_실패_재고부족() {
//        // Given
//        Long userId = 1L;
//        Long productId = 100L;
//        int quantity = 5;
//        int stock = 2; // 재고 부족
//        int price = 1000;
//
//        User user = new User(userId, 10000);
//        Product product = new Product(productId, "상품B", price, stock);
//        OrderRequest request = new OrderRequest(userId,
//                List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
//
//        given(userService.getPointByUserId(userId)).willReturn(user);
//        given(productService.getProductById(productId)).willReturn(product);
//
//        // When & Then
//        IllegalStateException exception = assertThrows(IllegalStateException.class,
//                () -> orderFacade.placeOrder(request));
//        assertTrue(exception.getMessage().contains("재고가 부족"));
//    }
//
//    @Test
//    void 주문_실패_포인트부족_재고롤백() {
//        // Given
//        Long userId = 1L;
//        Long productId = 100L;
//        int quantity = 3;
//        int unitPrice = 1000;
//        int stock = 10;
//        int userPoint = 1000; // 부족
//
//        Product product = new Product(productId, "상품C", unitPrice, stock);
//        User user = new User(userId, userPoint);
//
//        OrderRequest request = new OrderRequest(userId,
//                List.of(new OrderRequest.OrderItemRequest(productId, quantity)));
//
//        given(userService.getPointByUserId(userId)).willReturn(user);
//        given(productService.getProductById(productId)).willReturn(product);
//
//        // When & Then
//        IllegalStateException exception = assertThrows(IllegalStateException.class,
//                () -> orderFacade.placeOrder(request));
//
//        assertTrue(exception.getMessage().contains("잔액이 부족"));
//        verify(stockRollbackService).rollback(anyList());
//    }
//
//    @Test
//    void 쿠폰_적용_성공_시_주문_정상_처리() {
//        // Given
//        Long userId = 1L;
//        Long couponId = 10L;
//        Product product = new Product(1L, "상품A", 10000, 10000); // (id, name, stock, price)
//        User user = new User(userId, 20000); // 포인트 2만원
//        Coupon coupon = new Coupon(userId, "WELCOME10", 10);
//
//        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
//        ReflectionTestUtils.setField(itemReq, "productId", 1L);
//        ReflectionTestUtils.setField(itemReq, "quantity", 1);
//
//        OrderRequest request = new OrderRequest();
//        ReflectionTestUtils.setField(request, "userId", userId);
//        ReflectionTestUtils.setField(request, "items", List.of(itemReq));
//        ReflectionTestUtils.setField(request, "couponId", couponId);
//
//        Order expectedOrder = new Order(userId, List.of(new OrderItem(1L, 1, 10000)));
//
//        // Mocking
//        when(userService.getPointByUserId(userId)).thenReturn(user);
//        when(productService.getProductById(1L)).thenReturn(product);
//        when(couponService.getCouponById(couponId)).thenReturn(coupon);
//        when(orderService.saveOrder(eq(userId), anyList())).thenReturn(expectedOrder);
//
//        // When
//        Order order = orderFacade.placeOrder(request);
//
//        // Then
//        assertNotNull(order);
//        assertEquals(userId, order.getUserId());
//        assertEquals(1, order.getItems().size());
//        assertTrue(coupon.isUsed(), "쿠폰이 사용 처리되어야 한다");
//        assertEquals(11000, user.getPoint(), "포인트는 1만원 - 10% 할인(9천원) 차감되어 11000 남아야 함");
//    }
//
//    @Test
//    void 사용된_쿠폰_제출_시_예외_발생() {
//        // Given
//        Long userId = 1L;
//        Long couponId = 10L;
//        Product product = new Product(1L, "상품A", 100, 10000);
//        User user = new User(userId, 20000);
//        Coupon coupon = new Coupon(userId, "WELCOME10", 10);
//        coupon.use(); // 이미 사용 처리
//
//        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
//        ReflectionTestUtils.setField(itemReq, "productId", 1L);
//        ReflectionTestUtils.setField(itemReq, "quantity", 1);
//
//        OrderRequest request = new OrderRequest();
//        ReflectionTestUtils.setField(request, "userId", userId);
//        ReflectionTestUtils.setField(request, "items", List.of(itemReq));
//        ReflectionTestUtils.setField(request, "couponId", couponId);
//
//        when(userService.getPointByUserId(userId)).thenReturn(user);
//        when(productService.getProductById(1L)).thenReturn(product);
//        when(couponService.getCouponById(couponId)).thenReturn(coupon);
//
//        // When & Then
//        assertThrows(IllegalStateException.class, () -> orderFacade.placeOrder(request));
//    }
//
//    @Test
//    void 다른_사용자의_쿠폰_제출_시_예외_발생() {
//        // Given
//        Long userId = 1L;
//        Long couponId = 10L;
//        Product product = new Product(1L, "상품A", 100, 10000);
//        User user = new User(userId, 20000);
//        Coupon coupon = new Coupon(2L, "WELCOME10", 10); // 다른 userId
//
//        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
//        ReflectionTestUtils.setField(itemReq, "productId", 1L);
//        ReflectionTestUtils.setField(itemReq, "quantity", 1);
//
//        OrderRequest request = new OrderRequest();
//        ReflectionTestUtils.setField(request, "userId", userId);
//        ReflectionTestUtils.setField(request, "items", List.of(itemReq));
//        ReflectionTestUtils.setField(request, "couponId", couponId);
//
//        when(userService.getPointByUserId(userId)).thenReturn(user);
//        when(productService.getProductById(1L)).thenReturn(product);
//        when(couponService.getCouponById(couponId)).thenReturn(coupon);
//
//        // When & Then
//        assertThrows(IllegalStateException.class, () -> orderFacade.placeOrder(request));
//    }
}

