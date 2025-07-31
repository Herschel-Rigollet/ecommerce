package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.product.ProductRepository;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.product.Product;
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
