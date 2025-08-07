package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockRollbackService {

    private final ProductRepository productRepository;

    public void rollback(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("재고 복구 실패: 상품을 찾을 수 없습니다."));
            product.increaseStock(item.getQuantity());
        }
    }
}
