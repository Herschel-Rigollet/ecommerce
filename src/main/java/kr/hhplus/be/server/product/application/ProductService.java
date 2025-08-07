package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.repository.OrderItemRepository;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.presentation.dto.response.PopularProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 상품이 없습니다."));
    }

    // 상위 상품 5개 조회
    @Transactional(readOnly = true)
    public List<PopularProductResponse> getTop5PopularProducts() {
        // 1. 최근 3일 기간 설정
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(3);

        // 2. 해당 기간의 모든 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderDateBetween(startDate, endDate);

        // 3. 상품별 총 판매 수량 집계
        Map<Long, Integer> salesByProduct = orderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        // 4. 판매량 기준 상위 5개 선택 + 상품 정보 조회
        return salesByProduct.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Long productId = entry.getKey();
                    Integer totalSold = entry.getValue();

                    Product product = getProductById(productId);
                    return PopularProductResponse.from(product, totalSold);
                })
                .collect(Collectors.toList());
    }
}
