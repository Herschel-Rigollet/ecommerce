package kr.hhplus.be.server.product.presentation.dto.response;

import kr.hhplus.be.server.product.domain.Product;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
@Builder
public class ProductResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private int price;
    private int stock;

    public static ProductResponse from(Product product) {
        return ProductResponse
                .builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
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
                product.getProductId(),
                product.getProductName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
