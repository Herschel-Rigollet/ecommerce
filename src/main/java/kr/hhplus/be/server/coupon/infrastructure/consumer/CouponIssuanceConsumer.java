package kr.hhplus.be.server.coupon.infrastructure.consumer;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponPolicy;
import kr.hhplus.be.server.coupon.domain.repository.CouponPolicyRepository;
import kr.hhplus.be.server.coupon.domain.repository.CouponRepository;
import kr.hhplus.be.server.coupon.infrastructure.CouponEventProducer;
import kr.hhplus.be.server.coupon.presentation.dto.event.CouponRequestEvent;
import kr.hhplus.be.server.coupon.presentation.dto.event.CouponResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssuanceConsumer {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponEventProducer couponEventProducer;

    /**
     * 선착순 쿠폰 발급 처리
     * - 단일 Consumer로 순서 보장
     * - 수동 Acknowledgment로 정확한 처리 보장
     */
    @KafkaListener(
            topics = "coupon.issuance.requested",
            groupId = "coupon-issuance-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void processCouponIssuance(
            @Payload CouponRequestEvent event,
            Acknowledgment acknowledgment) {

        log.info("쿠폰 발급 요청 처리 시작: userId={}, code={}, requestId={}, thread={}",
                event.getUserId(), event.getCouponCode(), event.getRequestId(),
                Thread.currentThread().getName());

        try {
            // 1. 쿠폰 정책 조회
            CouponPolicy policy = couponPolicyRepository.findByCode(event.getCouponCode())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드: " + event.getCouponCode()));

            // 2. 현재 발급된 쿠폰 수 확인 (DB 기준)
            long currentCount = couponRepository.countByCode(event.getCouponCode());

            if (currentCount >= policy.getMaxCount()) {
                // 재고 부족 - 실패 결과 발행
                CouponResultEvent failResult = CouponResultEvent.failure(
                        event.getUserId(), event.getCouponCode(),
                        "쿠폰이 모두 소진되었습니다 (" + currentCount + "/" + policy.getMaxCount() + ")"
                );
                couponEventProducer.publishCouponResult(failResult);

                log.info("쿠폰 재고 부족으로 발급 실패: userId={}, code={}, currentCount={}",
                        event.getUserId(), event.getCouponCode(), currentCount);

                acknowledgment.acknowledge(); // 실패도 정상 처리로 간주
                return;
            }

            // 3. 중복 발급 체크
            boolean alreadyIssued = couponRepository.existsByUserIdAndCode(event.getUserId(), event.getCouponCode());
            if (alreadyIssued) {
                CouponResultEvent failResult = CouponResultEvent.failure(
                        event.getUserId(), event.getCouponCode(), "이미 발급받은 쿠폰입니다"
                );
                couponEventProducer.publishCouponResult(failResult);

                log.info("중복 발급으로 실패: userId={}, code={}", event.getUserId(), event.getCouponCode());
                acknowledgment.acknowledge();
                return;
            }

            // 4. 쿠폰 발급
            Coupon coupon = Coupon.builder()
                    .userId(event.getUserId())
                    .code(policy.getCode())
                    .discountRate(policy.getDiscountRate())
                    .used(false)
                    .issuedAt(LocalDateTime.now())
                    .expirationDate(LocalDateTime.now().plusDays(30))
                    .build();

            Coupon savedCoupon = couponRepository.save(coupon);

            // 5. 성공 결과 발행
            CouponResultEvent successResult = CouponResultEvent.success(
                    event.getUserId(), event.getCouponCode(), savedCoupon.getCouponId()
            );
            couponEventProducer.publishCouponResult(successResult);

            log.info("쿠폰 발급 성공: userId={}, code={}, couponId={}, 발급순서={}",
                    event.getUserId(), event.getCouponCode(), savedCoupon.getCouponId(), currentCount + 1);

            acknowledgment.acknowledge(); // 성공 시 처리 완료

        } catch (Exception e) {
            log.error("쿠폰 발급 처리 중 오류: userId={}, code={}, error={}",
                    event.getUserId(), event.getCouponCode(), e.getMessage());

            // 오류 발생 시 acknowledge 하지 않음 → 자동 재처리
            CouponResultEvent failResult = CouponResultEvent.failure(
                    event.getUserId(), event.getCouponCode(), "시스템 오류로 발급 실패"
            );

            try {
                couponEventProducer.publishCouponResult(failResult);
            } catch (Exception publishError) {
                log.error("실패 결과 발행 중 오류: {}", publishError.getMessage());
            }
        }
    }
}