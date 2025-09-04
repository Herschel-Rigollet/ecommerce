package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.presentation.dto.event.CouponRequestEvent;
import kr.hhplus.be.server.coupon.presentation.dto.event.CouponResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String COUPON_REQUEST_TOPIC = "coupon.issuance.requested";
    private static final String COUPON_RESULT_TOPIC = "coupon.issuance.completed";

    /**
     * 선착순 쿠폰 발급 요청 발행
     */
    public void publishCouponRequest(CouponRequestEvent event) {
        try {
            log.info("쿠폰 발급 요청 이벤트 발행: userId={}, code={}, requestId={}",
                    event.getUserId(), event.getCouponCode(), event.getRequestId());

            kafkaTemplate.send(COUPON_REQUEST_TOPIC, 0,
                            event.getRequestTime().toString(), event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("쿠폰 요청 이벤트 발행 실패: userId={}, code={}, error={}",
                                    event.getUserId(), event.getCouponCode(), throwable.getMessage());
                        } else {
                            log.info("쿠폰 요청 이벤트 발행 성공: userId={}, offset={}",
                                    event.getUserId(), result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            log.error("쿠폰 요청 이벤트 발행 중 예외: userId={}, code={}, error={}",
                    event.getUserId(), event.getCouponCode(), e.getMessage());
            throw new RuntimeException("쿠폰 발급 요청 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 쿠폰 발급 결과 발행
     * 사용자별 파티셔닝으로 병렬 처리
     */
    public void publishCouponResult(CouponResultEvent event) {
        try {
            kafkaTemplate.send(COUPON_RESULT_TOPIC, event.getUserId().toString(), event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("쿠폰 결과 이벤트 발행 실패: userId={}, error={}",
                                    event.getUserId(), throwable.getMessage());
                        } else {
                            log.info("쿠폰 결과 이벤트 발행 성공: userId={}, success={}",
                                    event.getUserId(), event.isSuccess());
                        }
                    });

        } catch (Exception e) {
            log.error("쿠폰 결과 이벤트 발행 중 예외: userId={}, error={}",
                    event.getUserId(), e.getMessage());
        }
    }
}