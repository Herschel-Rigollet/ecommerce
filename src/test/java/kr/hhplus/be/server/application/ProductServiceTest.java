package kr.hhplus.be.server.application;

import kr.hhplus.be.server.application.product.ProductRepository;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.presentation.product.response.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void 상품_목록을_정상적으로_조회할_수_있다() {
        // Given
        List<Product> mockProducts = List.of(
                new Product(1L, "상품A", 1000, 10),
                new Product(2L, "상품B", 2000, 5)
        );
        when(productRepository.findAll()).thenReturn(mockProducts);

        // When
        List<ProductResponse> responses = productService.getAllProducts();

        // Then
        assertEquals(2, responses.size());
        assertEquals("상품A", responses.get(0).name());
    }

    @Test
    void 상품ID로_상품을_상세_조회할_수_있다() {
        // Given
        Product product = new Product(1L, "상품1", 1000, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When
        ProductResponse response = productService.getProductById(1L);

        // Then
        assertEquals("상품1", response.name());
    }
}
