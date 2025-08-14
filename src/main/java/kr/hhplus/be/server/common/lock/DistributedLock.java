package kr.hhplus.be.server.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 락 키 (SpEL 표현식 지원)
     * 단일락: "'order:user:' + #request.userId"
     * 멀티락: "#request.items.![productId]" (상품 ID 리스트 반환)
     */
    String key();

    /**
     * 멀티락 사용 여부
     */
    boolean multiLock() default false;

    /**
     * 멀티락 키 접두사 (multiLock=true일 때 사용)
     */
    String keyPrefix() default "MULTI_LOCK:";

    /**
     * 락 대기시간 (초)
     */
    long waitTime() default 5L;

    /**
     * 락 점유시간 (초)
     */
    long leaseTime() default 10L;

    /**
     * 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 락 획득 실패 시 예외 메시지
     */
    String failMessage() default "락 획득에 실패했습니다.";
}
