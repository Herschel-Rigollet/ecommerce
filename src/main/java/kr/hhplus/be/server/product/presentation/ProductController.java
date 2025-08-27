package kr.hhplus.be.server.product.presentation;

import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.common.CommonResponse;
import kr.hhplus.be.server.common.CommonResultCode;
import kr.hhplus.be.server.product.presentation.dto.response.PopularProductResponse;
import kr.hhplus.be.server.product.presentation.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<CommonResponse> getProductById(@PathVariable(name = "productId") Long productId) {
        Product product = productService.getProductById(productId);
        return ResponseEntity.ok(CommonResponse.of(CommonResultCode.GET_PRODUCT_SUCCESS, ProductResponse.from(product)));
    }

    // 최근 3일 간 상위 상품 5개 조회
    @GetMapping("/popular")
    public ResponseEntity<CommonResponse> getPopularProducts() {
        // Redis 기반 조회 시도
        List<PopularProductResponse> popularProducts = productService.getPopularProductsFromRedis();

        // Redis 데이터가 없으면 기존 DB 방식으로 폴백
        if (popularProducts.isEmpty()) {
            popularProducts = productService.getTop5PopularProducts();
        }

        // 이미 ProductService에서 순위를 매겼으므로 순위 매기는 기능 삭제

        return ResponseEntity.ok(
                CommonResponse.of(CommonResultCode.GET_POPULAR_PRODUCTS_SUCCESS, popularProducts)
        );
    }
}
