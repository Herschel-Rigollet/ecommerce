package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.order.domain.repository.OrderItemRepository;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.presentation.dto.response.PopularProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    @DisplayName("상품 조회 성공")
    void getProductById_Success() {
        // Given
        Long productId = 1L;
        Product product = createProduct(productId, "iPhone 15", 1000000, 10);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // When
        Product result = productService.getProductById(productId);

        // Then
        assertThat(result).isEqualTo(product);
        assertThat(result.getProductName()).isEqualTo("iPhone 15");
        assertThat(result.getPrice()).isEqualTo(1000000);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
    void getProductById_NotFound_ThrowsException() {
        // Given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당 상품이 없습니다.");
    }

    @Test
    @DisplayName("null ID로 상품 조회 시 예외 발생")
    void getProductById_NullId_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> productService.getProductById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품 ID는 null일 수 없습니다.");
    }

    @Test
    @DisplayName("인기 상품 TOP5 조회 성공")
    void getTop5PopularProducts_Success() {
        // Given
        List<OrderItem> orderItems = Arrays.asList(
                createOrderItem(1L, 1L, 5), // 상품1: 5개 판매
                createOrderItem(2L, 2L, 3), // 상품2: 3개 판매
                createOrderItem(3L, 1L, 2), // 상품1: 추가 2개 (총 7개)
                createOrderItem(4L, 3L, 8)  // 상품3: 8개 판매
        );

        Product product1 = createProduct(1L, "iPhone 15", 1000000, 10);
        Product product2 = createProduct(2L, "Galaxy S24", 900000, 15);
        Product product3 = createProduct(3L, "iPad Pro", 1200000, 5);

        // Mock 설정
        given(orderItemRepository.findByOrderDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(orderItems);
        given(productRepository.findById(1L)).willReturn(Optional.of(product1));
        given(productRepository.findById(2L)).willReturn(Optional.of(product2));
        given(productRepository.findById(3L)).willReturn(Optional.of(product3));

        // When
        List<PopularProductResponse> result = productService.getTop5PopularProducts();

        // Then
        assertThat(result).hasSize(3);

        // 판매량 순으로 정렬되어야 함: 상품3(8개) > 상품1(7개) > 상품2(3개)
        assertThat(result.get(0).getProductId()).isEqualTo(3L);
        assertThat(result.get(0).getTotalSold()).isEqualTo(8);
        assertThat(result.get(0).getProductName()).isEqualTo("iPad Pro");

        assertThat(result.get(1).getProductId()).isEqualTo(1L);
        assertThat(result.get(1).getTotalSold()).isEqualTo(7); // 5 + 2
        assertThat(result.get(1).getProductName()).isEqualTo("iPhone 15");

        assertThat(result.get(2).getProductId()).isEqualTo(2L);
        assertThat(result.get(2).getTotalSold()).isEqualTo(3);
        assertThat(result.get(2).getProductName()).isEqualTo("Galaxy S24");
    }

    @Test
    @DisplayName("판매 기록이 없을 때 빈 리스트 반환")
    void getTop5PopularProducts_NoSales_ReturnsEmptyList() {
        // Given
        given(orderItemRepository.findByOrderDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(Arrays.asList()); // 빈 리스트

        // When
        List<PopularProductResponse> result = productService.getTop5PopularProducts();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("5개 이상의 상품이 있을 때 상위 5개만 반환")
    void getTop5PopularProducts_MoreThan5Products_ReturnsTop5Only() {
        // Given - 6개 상품의 판매 기록
        List<OrderItem> orderItems = Arrays.asList(
                createOrderItem(1L, 1L, 10), // 상품1: 10개 (1위)
                createOrderItem(2L, 2L, 9),  // 상품2: 9개 (2위)
                createOrderItem(3L, 3L, 8),  // 상품3: 8개 (3위)
                createOrderItem(4L, 4L, 7),  // 상품4: 7개 (4위)
                createOrderItem(5L, 5L, 6),  // 상품5: 6개 (5위)
                createOrderItem(6L, 6L, 5)   // 상품6: 5개 (6위, TOP5에서 제외됨)
        );

        given(orderItemRepository.findByOrderDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(orderItems);

        given(productRepository.findById(1L)).willReturn(Optional.of(createProduct(1L, "Product 1", 10000, 10)));
        given(productRepository.findById(2L)).willReturn(Optional.of(createProduct(2L, "Product 2", 20000, 10)));
        given(productRepository.findById(3L)).willReturn(Optional.of(createProduct(3L, "Product 3", 30000, 10)));
        given(productRepository.findById(4L)).willReturn(Optional.of(createProduct(4L, "Product 4", 40000, 10)));
        given(productRepository.findById(5L)).willReturn(Optional.of(createProduct(5L, "Product 5", 50000, 10)));

        // When
        List<PopularProductResponse> result = productService.getTop5PopularProducts();

        // Then
        assertThat(result).hasSize(5); // 정확히 5개만

        // 판매량 순서 확인
        assertThat(result.get(0).getProductId()).isEqualTo(1L);
        assertThat(result.get(0).getTotalSold()).isEqualTo(10);

        assertThat(result.get(1).getProductId()).isEqualTo(2L);
        assertThat(result.get(1).getTotalSold()).isEqualTo(9);

        assertThat(result.get(2).getProductId()).isEqualTo(3L);
        assertThat(result.get(2).getTotalSold()).isEqualTo(8);

        assertThat(result.get(3).getProductId()).isEqualTo(4L);
        assertThat(result.get(3).getTotalSold()).isEqualTo(7);

        assertThat(result.get(4).getProductId()).isEqualTo(5L);
        assertThat(result.get(4).getTotalSold()).isEqualTo(6);

        assertThat(result.stream().noneMatch(p -> p.getProductId().equals(6L))).isTrue();
    }

    @Test
    @DisplayName("같은 상품의 여러 주문이 합계되는지 확인")
    void getTop5PopularProducts_SameProductMultipleOrders_SumsCorrectly() {
        // Given - 같은 상품의 여러 주문
        List<OrderItem> orderItems = Arrays.asList(
                createOrderItem(1L, 1L, 3), // 상품1: 첫 번째 주문 3개
                createOrderItem(2L, 1L, 2), // 상품1: 두 번째 주문 2개
                createOrderItem(3L, 1L, 4), // 상품1: 세 번째 주문 4개
                createOrderItem(4L, 2L, 5)  // 상품2: 5개
        );

        Product product1 = createProduct(1L, "iPhone 15", 1000000, 10);
        Product product2 = createProduct(2L, "Galaxy S24", 900000, 15);

        given(orderItemRepository.findByOrderDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(orderItems);
        given(productRepository.findById(1L)).willReturn(Optional.of(product1));
        given(productRepository.findById(2L)).willReturn(Optional.of(product2));

        // When
        List<PopularProductResponse> result = productService.getTop5PopularProducts();

        // Then
        assertThat(result).hasSize(2);

        // 상품1이 더 많이 팔렸으므로 1위
        assertThat(result.get(0).getProductId()).isEqualTo(1L);
        assertThat(result.get(0).getTotalSold()).isEqualTo(9); // 3 + 2 + 4

        assertThat(result.get(1).getProductId()).isEqualTo(2L);
        assertThat(result.get(1).getTotalSold()).isEqualTo(5);
    }

    private Product createProduct(Long id, String name, int price, int stock) {
        Product product = new Product();
        product.setProductId(id);
        product.setProductName(name);
        product.setPrice(price);
        product.setStock(stock);
        return product;
    }

    private OrderItem createOrderItem(Long orderItemId, Long productId, int quantity) {
        OrderItem item = new OrderItem(productId, quantity, 1000);
        return item;
    }
}
