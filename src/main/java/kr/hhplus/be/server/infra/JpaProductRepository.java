package kr.hhplus.be.server.infra;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaProductRepository implements ProductRepository {
    private final SpringDataJpaProductRepository jpa;
    public JpaProductRepository(SpringDataJpaProductRepository jpa) { this.jpa = jpa; }

    @Override
    public List<Product> findAll() {
        return jpa.findAll();
    }
}
