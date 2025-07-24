package kr.hhplus.be.server.mapper;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.ProductDetailResponse;
import kr.hhplus.be.server.dto.ProductResponse;

import java.util.List;

public class ProductMapper {
    public static ProductResponse toDto(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
    }

    public static List<ProductResponse> toDtoList(List<Product> products) {
        return products.stream().map(ProductMapper::toDto).toList();
    }

    public static ProductDetailResponse toDetailDto(Product p) {
        return new ProductDetailResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
    }
}
