package kr.hhplus.be.server.product.infrastructure.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 재고 관리: 비관적 락
public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
