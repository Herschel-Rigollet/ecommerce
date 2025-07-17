package kr.hhplus.be.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductResponse {
    private Long productId;
    private String productName;
    private Long price;
    private int stock;
}