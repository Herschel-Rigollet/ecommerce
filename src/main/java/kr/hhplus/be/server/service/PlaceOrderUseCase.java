package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.infra.DataPlatformSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderUseCase {
    private final ProductRepository productRepository;
    private final BalanceRepository balanceRepository;
    private final OrderRepository orderRepository;
    private final DataPlatformSender sender;

    public PlaceOrderUseCase(ProductRepository productRepository,
                             BalanceRepository balanceRepository,
                             OrderRepository orderRepository,
                             DataPlatformSender sender) {
        this.productRepository = productRepository;
        this.balanceRepository = balanceRepository;
        this.orderRepository = orderRepository;
        this.sender = sender;
    }

    @Transactional
    public Order placeOrder(Long userId, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("재고가 부족합니다.");
        }

        long totalAmount = product.getPrice() * quantity;

        User balance = balanceRepository.findByUserId(userId);
        if (balance.getAmount() < totalAmount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 도메인 차감 로직
        product.decreaseStock(quantity);
        balance.use(totalAmount);

        Order order = new Order(userId, productId, quantity, totalAmount);
        orderRepository.save(order);
        sender.send(order);

        return order;
    }
}
