package kr.hhplus.be.server.common.lock.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AopForTransaction {

    /**
     * 락과 별도로 트랜잭션을 관리
     * 순서: 락 획득 -> 트랜잭션 시작 -> 비즈니스 로직 -> 트랜잭션 종료 -> 락 해제
     */
    @Transactional
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}