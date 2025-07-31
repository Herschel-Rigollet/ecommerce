package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void 상품_전체_목록을_조회할_수_있다() {
        // Given
        List<Product> mockProducts = List.of(
                new Product(1L, "상품A", 1000, 10),
                new Product(2L, "상품B", 2000, 5)
        );
        when(productRepository.findAll()).thenReturn(mockProducts);

        // When
        List<Product> result = productService.getAllProducts();

        // Then
        assertEquals(2, result.size());
        assertEquals("상품A", result.get(0).getName());
    }

    @Test
    void 상품ID로_단일_상품을_조회할_수_있다() {
        // Given
        Product product = new Product(1L, "상품C", 3000, 3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When
        Product result = productService.getProductById(1L);

        // Then
        assertEquals("상품C", result.getName());
        assertEquals(3000, result.getPrice());
    }

    @Test
    void 존재하지_않는_상품ID로_조회시_예외가_발생한다() {
        // Given
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            productService.getProductById(999L);
        });
    }

    @Test
    void null_ID로_조회시_예외가_발생한다() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            productService.getProductById(null);
        });
    }

    @Test
    void 저장소_내부_예외가_발생하면_전파된다() {
        // Given
        when(productRepository.findById(anyLong())).thenThrow(new RuntimeException("DB 오류"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            productService.getProductById(1L);
        });
    }

    @Test
    void 전체_상품_목록이_비어_있어도_예외없이_반환된다() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<Product> products = productService.getAllProducts();

        // Then
        assertNotNull(products);
        assertTrue(products.isEmpty());
    }
}

