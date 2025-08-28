package kr.hhplus.be.server.order.application.event;

import kr.hhplus.be.server.order.infrastructure.DataPlatformService;
import kr.hhplus.be.server.order.domain.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 주문 완료 이벤트 처리
     * - 트랜잭션 커밋 후 실행되어 데이터 일관성 보장
     * - 비동기 처리로 주문 성능에 영향 없음
     * - 실패해도 주문 트랜잭션에 영향 없음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("dataEventExecutor")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("주문 완료 이벤트 수신: orderId={}, userId={}, thread={}",
                event.getOrderId(), event.getUserId(), Thread.currentThread().getName());

        try {
            // 데이터 플랫폼으로 실시간 주문 정보 전송
            dataPlatformService.sendOrderDataToPlatform(event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("데이터 플랫폼 전송 최종 실패: orderId={}, error={}",
                                    event.getOrderId(), throwable.getMessage());
                            // 실패 처리는 DataPlatformService에서 담당
                        } else {
                            log.info("데이터 플랫폼 전송 최종 성공: orderId={}", event.getOrderId());
                        }
                    });

            // 필요시 다른 부가 로직들도 여기서 처리 가능
            // 예: 이메일 알림, SMS 발송, 통계 업데이트 등

        } catch (Exception e) {
            // 이벤트 처리 실패가 주문 트랜잭션에 영향을 주지 않도록 예외 처리
            log.error("주문 완료 이벤트 처리 중 오류: orderId={}, error={}",
                    event.getOrderId(), e.getMessage());
        }
    }

    /**
     * 추가 이벤트 처리 예시
     * 주문 완료 후 필요한 다른 부가 로직들
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor") // 별도의 스레드 풀 사용 가능
    public void handleOrderNotification(OrderCompletedEvent event) {
        log.info("주문 알림 처리: orderId={}", event.getOrderId());

        // 이메일, SMS 등 알림 발송 로직
        // sendOrderConfirmationEmail(event);
        // sendOrderConfirmationSMS(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("analyticsExecutor")
    public void handleOrderAnalytics(OrderCompletedEvent event) {
        log.info("주문 분석 데이터 처리: orderId={}", event.getOrderId());

        // 실시간 분석을 위한 데이터 처리
        // updateUserPurchaseHistory(event);
        // updateProductAnalytics(event);
        // updateSalesMetrics(event);
    }
}