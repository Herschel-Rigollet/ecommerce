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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    // Redis 키 패턴 정의
    private static final String POPULAR_PRODUCTS_KEY_PREFIX = "popular_products:";
    private static final int EXPIRY_DAYS = 4; // 4일 후 자동 삭제 (최근 3일 + 1일 여유)

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

    // 인기상품 데이터 업데이트
    public void updatePopularProductsData(List<OrderItem> orderItems) {
        String today = LocalDate.now().toString();
        String dailyKey = POPULAR_PRODUCTS_KEY_PREFIX + today;

        try {
            // 여러 명령어를 한 번에 전송하여 네트워크 비용 절약
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {

                // 1. 각 상품별로 ZINCRBY 명령어 실행 (판매량 누적)
                for (OrderItem item : orderItems) {
                    connection.zIncrBy(
                            dailyKey.getBytes(),
                            item.getQuantity(),
                            item.getProductId().toString().getBytes()
                    );
                }

                // 2. TTL 설정 (메모리 관리)
                connection.expire(dailyKey.getBytes(), Duration.ofDays(EXPIRY_DAYS).getSeconds());

                return null; // executePipelined는 return 값 무시
            });

            log.info("파이프라인 인기상품 업데이트 완료: date={}, items={}", today, orderItems.size());

        } catch (Exception e) {
            // Redis 실패가 주문 처리에 영향을 주지 않도록 예외를 로그만 남김
            log.error("파이프라인 인기상품 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    // Redis SortedSet 기반 인기상품 Top5 조회
    public List<PopularProductResponse> getPopularProductsFromRedis() {
        try {
            LocalDate today = LocalDate.now();

            // 최근 3일간의 Redis 키 생성 (오늘, 어제, 그저께)
            String[] dailyKeys = IntStream.range(0, 3)
                    .mapToObj(today::minusDays)
                    .map(date -> POPULAR_PRODUCTS_KEY_PREFIX + date.toString())
                    .toArray(String[]::new);

            // 임시 집계 키 생성 (중복 방지를 위해 timestamp 추가)
            String tempKey = "popular_products:temp:" + System.currentTimeMillis();

            try {
                List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {

                    // 1. ZUNIONSTORE: 3일간 데이터 합집합 (점수 합산)
                    byte[][] keyBytes = Arrays.stream(dailyKeys)
                            .map(String::getBytes)
                            .toArray(byte[][]::new);
                    connection.zUnionStore(tempKey.getBytes(), keyBytes);

                    // 2. ZREVRANGE WITHSCORES: 상위 5개 조회 (내림차순)
                    // Redis가 자동으로 점수 기준 정렬하므로 상위 5개만 바로 조회
                    connection.zRevRangeWithScores(tempKey.getBytes(), 0, 4);

                    // 3. DEL: 임시 키 삭제 (메모리 누수 방지)
                    connection.del(tempKey.getBytes());

                    return null;
                });

                // 파이프라인 결과에서 랭킹 데이터 추출 (두 번째 명령어 결과)
                if (results != null && results.size() >= 2) {
                    @SuppressWarnings("unchecked")
                    Set<ZSetOperations.TypedTuple<Object>> rankingData =
                            (Set<ZSetOperations.TypedTuple<Object>>) results.get(1);

                    return convertToPopularProductResponse(rankingData);
                }

                log.warn("Redis에서 인기상품 데이터 없음. 기존 DB 방식 사용 권장");
                return Collections.emptyList();

            } finally {
                // 예외 발생 시에도 임시 키 정리 보장
                stringRedisTemplate.delete(tempKey);
            }

        } catch (Exception e) {
            log.error("Redis 인기상품 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PopularProductResponse> convertToPopularProductResponse(
            Set<ZSetOperations.TypedTuple<Object>> rankingData) {

        List<PopularProductResponse> results = new ArrayList<>();

        try {
            // Redis ZREVRANGE WITHSCORES 결과는 이미 점수 순으로 정렬되어 있음
            for (ZSetOperations.TypedTuple<Object> tuple : rankingData) {
                String productIdStr = tuple.getValue().toString();
                Double score = tuple.getScore();

                Long productId = Long.valueOf(productIdStr);
                Integer totalSold = score.intValue();

                // DB에서 상품 상세 정보 조회
                Product product = getProductById(productId);
                results.add(PopularProductResponse.from(product, totalSold));
            }

            log.info("Redis 랭킹 데이터 변환 완료: {} 개", results.size());

        } catch (Exception e) {
            log.error("랭킹 데이터 변환 실패: {}", e.getMessage());
        }

        return results;
    }
}
