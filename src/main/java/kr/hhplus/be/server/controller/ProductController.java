package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.hhplus.be.server.dto.ProductResponse;
import kr.hhplus.be.server.mapper.ProductMapper;
import kr.hhplus.be.server.service.ProductQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductQueryService service;

    public ProductController(ProductQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "전체 상품 목록 조회", description = "현재 구매 가능한 전체 상품 목록을 조회합니다.")
    public List<ProductResponse> getProducts() {
        return ProductMapper.toDtoList(service.getAllProducts());
    }
}
