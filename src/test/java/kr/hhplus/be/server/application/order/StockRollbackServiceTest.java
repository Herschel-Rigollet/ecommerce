package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductRepository;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.presentation.order.request.OrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockRollbackServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockRollbackService stockRollbackService;

    @Test
    void 재고를_정상적으로_복구한다() {
        // Given
        Long productId = 1L;
        int rollbackQty = 3;

        Product product = new Product(productId, "상품A", 5000, 2); // 재고 2
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        List<OrderItem> items = List.of(
                new OrderItem(productId, rollbackQty, 5000)
        );

        // When
        stockRollbackService.rollback(items);

        // Then
        assertEquals(5, product.getStock()); // 2 + 3
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void 존재하지_않는_상품이면_예외가_발생한다() {
        // Given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        List<OrderItem> items = List.of(
                new OrderItem(productId, 2, 5000)
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> stockRollbackService.rollback(items));
    }
}

