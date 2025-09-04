package kr.hhplus.be.server.order.application.event;

import kr.hhplus.be.server.order.infrastructure.DataPlatformService;
import kr.hhplus.be.server.order.domain.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final DataPlatformService dataPlatformService;

    /**
     * Kafka로 주문 완료 이벤트 처리
     * - 분산 환경에서 안정적인 메시지 처리
     * - 재시도 및 에러 처리 내장
     * - 확장성과 내구성 보장
     */
    @KafkaListener(topics = "order.completed", groupId = "order-data-platform-group")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("주문 완료 이벤트 수신: orderId={}, userId={}, thread={}",
                event.getOrderId(), event.getUserId(), Thread.currentThread().getName());

        try {
            // 데이터 플랫폼으로 실시간 주문 정보 전송
            dataPlatformService.sendOrderDataToPlatform(event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("데이터 플랫폼 전송 최종 실패: orderId={}, eventId={}, error={}",
                                    event.getOrderId(), event.getEventId(), throwable.getMessage());
                        } else {
                            log.info("데이터 플랫폼 전송 최종 성공: orderId={}, eventId={}",
                                    event.getOrderId(), event.getEventId());
                        }
                    });

        } catch (Exception e) {
            // 이벤트 처리 실패가 주문 트랜잭션에 영향을 주지 않도록 예외 처리
            log.error("주문 완료 Kafka 메시지 처리 중 오류: orderId={}, eventId={}, error={}",
                    event.getOrderId(), event.getEventId(), e.getMessage());
        }
    }

    /**
     * 알림 처리용 별도 Consumer
     */
    @KafkaListener(topics = "order.completed", groupId = "order-notification-group")
    public void handleOrderNotification(OrderCompletedEvent event) {
        log.info("주문 알림 Kafka 메시지 처리: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());

        // 이메일, SMS 등 알림 발송 로직
        // sendOrderConfirmationEmail(event);
        // sendOrderConfirmationSMS(event);
    }

    /**
     * 분석 데이터 처리용 별도 Consumer
     */
    @KafkaListener(topics = "order.completed", groupId = "order-analytics-group")
    public void handleOrderAnalytics(OrderCompletedEvent event) {
        log.info("주문 분석 Kafka 메시지 처리: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());

        // 실시간 분석을 위한 데이터 처리
        // updateUserPurchaseHistory(event);
        // updateProductAnalytics(event);
        // updateSalesMetrics(event);
    }
}