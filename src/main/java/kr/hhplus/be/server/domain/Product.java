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
