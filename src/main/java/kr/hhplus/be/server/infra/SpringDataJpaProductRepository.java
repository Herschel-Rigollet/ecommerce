package kr.hhplus.be.server.infra;

import kr.hhplus.be.server.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaProductRepository extends JpaRepository<Product, Long> {
}
