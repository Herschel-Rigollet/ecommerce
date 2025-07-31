package kr.hhplus.be.server.domain.product;


public class Product {

    private Long id;
    private String name;
    private int price;
    private int stock;

    public Product(Long id, String name, int price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getStock() { return stock; }

    public void setId(Long id) { this.id = id; }
}