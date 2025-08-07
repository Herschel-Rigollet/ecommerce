package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockRollbackService {

    private final ProductRepository productRepository;

    @Transactional
    public void rollback(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("재고 복구 실패: 상품을 찾을 수 없습니다."));
            product.increaseStock(item.getQuantity());
        }
    }

    // 비관적 락으로 상품을 다시 조회해서 안전하게 복구
    // @param rollbackTargets 복구할 주문 아이템들
    // @param lockedProducts 이미 락을 획득한 상품들
    @Transactional
    public void rollbackSafely(List<OrderItem> rollbackTargets, List<Product> lockedProducts) {
        try {
            for (OrderItem item : rollbackTargets) {
                // 이미 락을 획득한 상품에서 찾기
                Product product = lockedProducts.stream()
                        .filter(p -> p.getProductId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

                if (product != null) {
                    product.increaseStock(item.getQuantity());
                    System.out.println("재고 복구: 상품ID " + product.getProductId() +
                            ", 복구 수량: " + item.getQuantity() +
                            ", 복구 후 재고: " + product.getStock());
                } else {
                    Product lockedProduct = productRepository.findByIdForUpdate(item.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("재고 복구 실패: 상품을 찾을 수 없습니다."));

                    lockedProduct.increaseStock(item.getQuantity());
                    System.out.println("안전장치 재고 복구: 상품ID " + lockedProduct.getProductId() +
                            ", 복구 수량: " + item.getQuantity());
                }
            }
        } catch (Exception rollbackException) {
            System.err.println("재고 복구 실패: " + rollbackException.getMessage());
        }
    }
}
