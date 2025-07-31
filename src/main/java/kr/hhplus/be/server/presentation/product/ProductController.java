package kr.hhplus.be.server.presentation.product;

import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.presentation.common.CommonResponse;
import kr.hhplus.be.server.presentation.common.CommonResultCode;
import kr.hhplus.be.server.presentation.product.response.ProductResponse;
import kr.hhplus.be.server.presentation.user.response.UserResponse;
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
