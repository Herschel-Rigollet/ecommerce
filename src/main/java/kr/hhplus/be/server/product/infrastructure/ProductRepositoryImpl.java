package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.infrastructure.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;


    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll();
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long productId) {
        return productJpaRepository.findByIdForUpdate(productId);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }
}
