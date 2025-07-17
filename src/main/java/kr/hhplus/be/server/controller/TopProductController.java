package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/popular-products")
@Tag(name = "Popular Product API", description = "인기 판매 상품 조회 API")
public class TopProductController {
    @Operation(summary = "인기 상품 조회", description = "최근 3일간 가장 많이 팔린 상위 5개 상품을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getPopularProducts() {
        return ResponseEntity.ok(List.of(
                new ProductResponse(1L, "인기상품1", 3000L, 50),
                new ProductResponse(2L, "인기상품2", 1500L, 30)
        ));
    }
}
