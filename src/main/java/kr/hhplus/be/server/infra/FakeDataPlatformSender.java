package kr.hhplus.be.server.infra;

import kr.hhplus.be.server.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class FakeDataPlatformSender implements DataPlatformSender {
    @Override
    public void send(Order order) {
        System.out.println("📡 [Mock] 주문 정보 전송 완료: " + order.getUserId() + ", " + order.getTotalAmount());
    }
}
