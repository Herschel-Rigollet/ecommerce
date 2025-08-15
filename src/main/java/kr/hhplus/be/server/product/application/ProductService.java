package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.repository.OrderItemRepository;
import kr.hhplus.be.server.product.domain.repository.ProductRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.presentation.dto.response.PopularProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 상품 저장 (멀티락 사용 시 명시적 저장 필요)
     */
    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 상품이 없습니다."));
    }

    // 비관적 락으로 상품 조회
    @Transactional
    public Product getProductByIdForUpdate(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
        }
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 상품이 없습니다: " + productId));
    }

    // 재고 차감
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        // 비관적 락으로 상품 조회
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 재고 검증 및 차감
        if (product.getStock() < quantity) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }

        product.decreaseStock(quantity);
        // 트랜잭션 끝나면 자동으로 락 해제
    }

    /**
     * 분산락을 적용한 재고 차감
     * 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 종료 → 락 해제
     */
    @DistributedLock(
            key = "#productId",
            waitTime = 3L,
            leaseTime = 5L,
            failMessage = "해당 상품의 재고 처리 중입니다. 잠시 후 다시 시도해주세요."
    )
    public void decreaseStockWithDistributedLock(Long productId, int quantity) {
        log.info("재고 차감 시작: productId={}, quantity={}, thread={}",
                productId, quantity, Thread.currentThread().getName());

        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 수량은 0보다 커야 합니다.");
        }

        // 일반 조회 (락은 이미 획득한 상태)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 재고 검증 및 차감
        if (product.getStock() < quantity) {
            throw new IllegalStateException(
                    String.format("재고가 부족합니다. 현재 재고: %d, 요청 수량: %d",
                            product.getStock(), quantity)
            );
        }

        product.decreaseStock(quantity);
        productRepository.save(product);

        log.info("재고 차감 완료: productId={}, 차감수량={}, 남은재고={}, thread={}",
                productId, quantity, product.getStock(), Thread.currentThread().getName());
    }

    /**
     * 재고 증가 (복구용)
     */
    @DistributedLock(
            key = "#productId",
            waitTime = 3L,
            leaseTime = 5L,
            failMessage = "재고 복구 처리 중입니다. 잠시 후 다시 시도해주세요."
    )
    public void increaseStockWithDistributedLock(Long productId, int quantity) {
        log.info("재고 증가 시작: productId={}, quantity={}, thread={}",
                productId, quantity, Thread.currentThread().getName());

        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 null일 수 없습니다.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("증가할 수량은 0보다 커야 합니다.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        product.increaseStock(quantity);
        productRepository.save(product);

        log.info("재고 증가 완료: productId={}, 증가수량={}, 총재고={}, thread={}",
                productId, quantity, product.getStock(), Thread.currentThread().getName());
    }

    // 상위 상품 5개 조회
    @Cacheable(cacheNames = "topProducts", key = "'last3days_top5'", sync = true)
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
