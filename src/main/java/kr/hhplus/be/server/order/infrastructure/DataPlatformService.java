package kr.hhplus.be.server.order.infrastructure;

import kr.hhplus.be.server.order.domain.event.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DataPlatformService {

    private final RestTemplate restTemplate;
    private static final String DATA_PLATFORM_URL = "https://example-dataplatform.com/api/orders";

    public DataPlatformService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 비동기로 데이터 플랫폼에 주문 정보 전송
     * 메인 트랜잭션과 완전 분리되어 실행됨
     */
    @Async("dataEventExecutor")
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Void> sendOrderDataToPlatform(OrderCompletedEvent event) {
        log.info("데이터 플랫폼 전송 시작: orderId={}, eventId={}, thread={}",
                event.getOrderId(), event.getEventId(), Thread.currentThread().getName());

        try {
            // Mock API 호출 데이터 구성
            Map<String, Object> orderData = buildOrderData(event);

            // HTTP 요청 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Event-Id", event.getEventId()); // 멱등성 보장
            headers.set("X-Source", "order-service");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderData, headers);

            // Mock API 호출 (실제로는 외부 데이터 플랫폼)
            ResponseEntity<String> response = callMockDataPlatformAPI(request);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("데이터 플랫폼 전송 성공: orderId={}, eventId={}, response={}",
                        event.getOrderId(), event.getEventId(), response.getBody());
            } else {
                throw new RuntimeException("데이터 플랫폼 응답 오류: " + response.getStatusCode());
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: orderId={}, eventId={}, error={}",
                    event.getOrderId(), event.getEventId(), e.getMessage());

            // 재시도 후에도 실패하면 별도 처리 (데드레터 큐 등)
            handleSendFailure(event, e);
            throw e; // 재시도를 위해 예외 재전파
        }
    }

    /**
     * Mock 데이터 플랫폼 API 호출
     * 실제 환경에서는 외부 시스템 URL로 변경
     */
    private ResponseEntity<String> callMockDataPlatformAPI(HttpEntity<Map<String, Object>> request) {
        try {
            // 실제로는 외부 API 호출
            // return restTemplate.postForEntity(DATA_PLATFORM_URL, request, String.class);

            // Mock 응답 시뮬레이션
            simulateNetworkLatency();

            // 10% 확률로 실패 시뮬레이션 (테스트 목적)
            if (Math.random() < 0.1) {
                throw new RuntimeException("Mock API 실패 시뮬레이션");
            }

            return ResponseEntity.ok("{\"status\":\"success\",\"message\":\"데이터 수신 완료\"}");

        } catch (Exception e) {
            log.error("Mock API 호출 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 주문 이벤트 데이터를 데이터 플랫폼 형식으로 변환
     */
    private Map<String, Object> buildOrderData(OrderCompletedEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("orderId", event.getOrderId());
        data.put("userId", event.getUserId());
        data.put("totalAmount", event.getTotalAmount());
        data.put("couponId", event.getCouponId());
        data.put("orderTime", event.getOrderTime().toString());
        data.put("orderItems", event.getOrderItems());
        data.put("eventType", "ORDER_COMPLETED");
        data.put("source", "order-service");

        return data;
    }

    /**
     * 전송 실패 처리 (데드레터 큐, 알림 등)
     */
    private void handleSendFailure(OrderCompletedEvent event, Exception e) {
        log.error("최종 전송 실패 처리: orderId={}, eventId={}", event.getOrderId(), event.getEventId());

        // 실제 환경에서는 다음과 같은 처리 구현:
        // 1. 데드레터 큐에 메시지 저장
        // 2. 관리자 알림 발송
        // 3. 실패 이벤트 테이블에 기록
        // 4. 수동 재처리를 위한 대기열 추가

        // 예시: 실패 로그를 별도 테이블에 저장
        saveFailedEvent(event, e.getMessage());
    }

    private void saveFailedEvent(OrderCompletedEvent event, String errorMessage) {
        // 실패한 이벤트를 별도 테이블에 저장하는 로직
        // 나중에 수동으로 재처리할 수 있도록 함
        log.info("실패 이벤트 저장: orderId={}, error={}", event.getOrderId(), errorMessage);
    }

    /**
     * 네트워크 지연 시뮬레이션
     */
    private void simulateNetworkLatency() {
        try {
            // 100-500ms 랜덤 지연
            Thread.sleep(100 + (long)(Math.random() * 400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}