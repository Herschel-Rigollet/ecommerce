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
        List<PopularProductResponse> popularProducts = productService.getTop5PopularProducts();

        // 순위 매기기 (1~5)
        AtomicInteger rank = new AtomicInteger(1);
        List<PopularProductResponse> rankedProducts = popularProducts.stream()
                .map(product -> product.withRank(rank.getAndIncrement()))
                .toList();

        return ResponseEntity.ok(
                CommonResponse.of(CommonResultCode.GET_POPULAR_PRODUCTS_SUCCESS, rankedProducts)
        );
    }
}
