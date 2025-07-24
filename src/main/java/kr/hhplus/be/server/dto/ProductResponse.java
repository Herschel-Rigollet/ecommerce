package kr.hhplus.be.server.dto;

public class ProductResponse {
    private Long id;
    private String name;
    private long price;
    private int stock;

    public ProductResponse(Long id, String name, long price, int stock) {
        this.id = id; this.name = name; this.price = price; this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }
}
