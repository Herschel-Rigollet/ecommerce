package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.presentation.product.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("해당 상품이 없습니다."));
    }

}
