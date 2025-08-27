package kr.hhplus.be.server.product.presentation.dto.response;

import kr.hhplus.be.server.product.domain.Product;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class PopularProductResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private int price;
    private int currentStock;
    private int totalSold; // 최근 3일간 판매량
    private int rank; // 순위 (1~5)

    public static PopularProductResponse from(Product product, int totalSold) {
        return PopularProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .price(product.getPrice())
                .currentStock(product.getStock())
                .totalSold(totalSold)
                .build();
    }

    // 순위 설정용
    public PopularProductResponse withRank(int rank) {
        return PopularProductResponse.builder()
                .productId(this.productId)
                .productName(this.productName)
                .price(this.price)
                .currentStock(this.currentStock)
                .totalSold(this.totalSold)
                .rank(rank)
                .build();
    }
}