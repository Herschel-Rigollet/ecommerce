package kr.hhplus.be.server.order.infrastructure.kafka;

import kr.hhplus.be.server.order.domain.event.OrderCompletedEvent;
import kr.hhplus.be.server.order.infrastructure.DataPlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataPlatformEventConsumer {

    private final DataPlatformService dataPlatformService;

    @KafkaListener(
            topics = "order.completed",
            groupId = "order-data-platform-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCompletedForDataPlatform(
            @Payload OrderCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("데이터 플랫폼용 메시지 수신: orderId={}, eventId={}, partition={}, offset={}",
                event.getOrderId(), event.getEventId(), partition, offset);

        try {
            dataPlatformService.sendOrderDataToPlatform(event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("데이터 플랫폼 전송 실패: orderId={}, error={}",
                                    event.getOrderId(), throwable.getMessage());
                            // 실패시 acknowledge 하지 않아서 재처리됨
                        } else {
                            log.info("데이터 플랫폼 전송 성공: orderId={}", event.getOrderId());
                            acknowledgment.acknowledge(); // 성공시에만 처리 완료
                        }
                    });

        } catch (Exception e) {
            log.error("Consumer 처리 중 오류: orderId={}, error={}", event.getOrderId(), e.getMessage());
        }
    }
}