package kr.hhplus.be.server.infra;

import kr.hhplus.be.server.domain.Order;

public interface DataPlatformSender {
    void send(Order order);
}
