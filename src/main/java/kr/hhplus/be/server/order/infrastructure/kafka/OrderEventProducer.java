package kr.hhplus.be.server.order.infrastructure.kafka;

import kr.hhplus.be.server.order.domain.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_COMPLETED_TOPIC = "order.completed";

    /**
     * 주문 완료 이벤트를 Kafka로 발행
     */
    public void publishOrderCompletedEvent(OrderCompletedEvent event) {
        try {
            log.info("주문 완료 이벤트 발행 시작: orderId={}, eventId={}",
                    event.getOrderId(), event.getEventId());

            kafkaTemplate.send(ORDER_COMPLETED_TOPIC, event.getOrderId().toString(), event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Kafka 메시지 발행 실패: orderId={}, eventId={}, error={}",
                                    event.getOrderId(), event.getEventId(), throwable.getMessage());
                            handlePublishFailure(event, throwable);
                        } else {
                            log.info("Kafka 메시지 발행 성공: orderId={}, eventId={}, partition={}, offset={}",
                                    event.getOrderId(), event.getEventId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 예외: orderId={}, eventId={}, error={}",
                    event.getOrderId(), event.getEventId(), e.getMessage());
            handlePublishFailure(event, e);
        }
    }

    /**
     * 발행 실패 처리 (fallback 메커니즘)
     */
    private void handlePublishFailure(OrderCompletedEvent event, Throwable throwable) {
        log.error("이벤트 발행 실패 - fallback 처리: orderId={}, eventId={}, error={}",
                event.getOrderId(), event.getEventId(), throwable.getMessage());

        saveFailedEvent(event, throwable.getMessage());
    }

    private void saveFailedEvent(OrderCompletedEvent event, String errorMessage) {
        log.info("실패 이벤트 저장: orderId={}, eventId={}", event.getOrderId(), event.getEventId());
        // 실패한 이벤트를 DB에 저장하는 로직
    }
}