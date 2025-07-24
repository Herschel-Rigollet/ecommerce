package kr.hhplus.be.server.dto;

public class ProductDetailResponse {
    private Long id;
    private String name;
    private long price;
    private int stock;

    public ProductDetailResponse(Long id, String name, long price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
