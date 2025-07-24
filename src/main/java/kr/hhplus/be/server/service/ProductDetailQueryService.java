package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class ProductDetailQueryService {
    private final ProductRepository repository;

    public ProductDetailQueryService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product getProductById(Long productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
    }
}

