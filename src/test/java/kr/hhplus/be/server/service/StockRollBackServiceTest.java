package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StockRollBackServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockRollbackService stockRollbackService;

    @Test
    @DisplayName("결제 실패 시 재고가 정상 복구된다")
    void rollbackStock_success() {
        // Given
        Long productId = 1L;
        int quantity = 3;
        Product product = new Product(1L, "상품명", 1000L, 5);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // When
        stockRollbackService.rollback(productId, quantity);

        // Then
        assertEquals(8, product.getStock());
        verify(productRepository).save(product);
    }
}
