package kr.hhplus.be.server.product.domain.repository;

import kr.hhplus.be.server.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);
    List<Product> findAll();
    Optional<Product> findByIdForUpdate(Long id);
    Product save(Product product);
}
