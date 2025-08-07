package kr.hhplus.be.server.product.presentation;

import kr.hhplus.be.server.product.application.ProductService;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.common.CommonResponse;
import kr.hhplus.be.server.common.CommonResultCode;
import kr.hhplus.be.server.product.presentation.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
