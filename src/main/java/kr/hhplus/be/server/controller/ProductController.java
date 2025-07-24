package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.hhplus.be.server.dto.ProductDetailResponse;
import kr.hhplus.be.server.dto.ProductResponse;
import kr.hhplus.be.server.mapper.ProductMapper;
import kr.hhplus.be.server.service.ProductDetailQueryService;
import kr.hhplus.be.server.service.ProductQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductQueryService queryService;
    private final ProductDetailQueryService detailService;

    public ProductController(ProductQueryService queryService, ProductDetailQueryService detailService) {
        this.queryService = queryService;
        this.detailService = detailService;
    }

    @GetMapping
    @Operation(summary = "전체 상품 목록 조회", description = "현재 구매 가능한 전체 상품 목록을 조회합니다.")
    public List<ProductResponse> getProducts() {
        return ProductMapper.toDtoList(queryService.getAllProducts());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회", description = "상품 ID를 기반으로 상품 상세 정보를 반환합니다.")
    public ProductDetailResponse getProductDetail(@PathVariable Long productId) {
        return ProductMapper.toDetailDto(detailService.getProductById(productId));
    }
}
