package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductQueryServiceTest {
    @Test
    void 전체_상품_목록을_조회한다() {
        ProductRepository repository = mock(ProductRepository.class);
        ProductQueryService service = new ProductQueryService(repository);

        List<Product> mockData = List.of(
                new Product(1L, "상품A", 1000, 10),
                new Product(2L, "상품B", 2000, 5)
        );
        when(repository.findAll()).thenReturn(mockData);

        List<Product> result = service.getAllProducts();

        assertEquals(2, result.size());
        assertEquals("상품A", result.get(0).getName());
    }
}
