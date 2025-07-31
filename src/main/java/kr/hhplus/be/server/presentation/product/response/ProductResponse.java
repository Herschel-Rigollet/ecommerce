package kr.hhplus.be.server.presentation.product.response;

import kr.hhplus.be.server.domain.product.Product;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductResponse {
    private Long productId;
    private String name;
    private int price;
    private int stock;

    public static ProductResponse from(Product product) {
        return ProductResponse
                .builder()
                .productId(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }

    public static List<ProductResponse> of(List<Product> productList){
        return productList.stream()
                .map(ProductResponse::of) // 단건 변환
                .toList();
    }

    // 단건용
    public static ProductResponse of(Product product){
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
