package kr.hhplus.be.server.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Product {
    @Id
    private Long id;
    private String name;
    private long price;
    private int stock;

    protected Product() {}  // JPA용

    public Product(Long id, String name, long price, int stock) {
        if (stock < 0) throw new IllegalArgumentException("재고는 음수일 수 없습니다.");
        this.id = id; this.name = name; this.price = price; this.stock = stock;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0 || stock < quantity) {
            throw new IllegalArgumentException("재고 부족 또는 잘못된 수량");
        }
        this.stock -= quantity;
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
