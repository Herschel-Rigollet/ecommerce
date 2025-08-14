package kr.hhplus.be.server.common.lock.aspect;

import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.common.lock.exception.DistributedLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // 트랜잭션(@Transactional)보다 먼저 실행되도록 높은 우선순위 설정
public class DistributedLockAspect {
    private final RedissonClient redissonClient;

    /**
     * 분산락 처리 - @Order(1)로 트랜잭션보다 먼저 실행
     * 실행 순서 (@Order를 통한 Aspect 체인):
     * 1. DistributedLockAspect.lock() - 락 획득 (@Order(1))
     * 2. TransactionInterceptor - 트랜잭션 시작 (Spring 기본 Order)
     * 3. 실제 비즈니스 로직 실행
     * 4. TransactionInterceptor - 트랜잭션 종료 (커밋/롤백)
     * 5. DistributedLockAspect.lock() finally - 락 해제
     */
    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String lockKey = LOCK_PREFIX + getDynamicValue(signature.getParameterNames(),
                joinPoint.getArgs(),
                distributedLock.key());

        RLock rLock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 시도
            boolean available = rLock.tryLock(distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit());

            if (!available) {
                log.warn("분산락 획득 실패: {}", lockKey);
                throw new DistributedLockException(distributedLock.failMessage());
            }

            log.info("분산락 획득 성공: {} (스레드: {})", lockKey, Thread.currentThread().getName());

            // 2. 다음 Aspect 체인 실행 (트랜잭션 Aspect -> 실제 메서드)
            // @Order(1)이므로 @Transactional Aspect가 이후에 실행됨
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            log.error("락 획득 중 인터럽트 발생: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new DistributedLockException("락 획득 중 인터럽트가 발생했습니다.", e);
        } finally {
            // 3. 락 해제 (트랜잭션 종료 후)
            try {
                if (rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                    log.info("분산락 해제 완료: {} (스레드: {})", lockKey, Thread.currentThread().getName());
                }
            } catch (IllegalMonitorStateException e) {
                log.warn("락 해제 실패 - 이미 해제되었거나 다른 스레드가 소유: {}", lockKey);
            }
        }
    }

    private Object getDynamicValue(String[] parameterNames, Object[] args, String key) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(key);
        EvaluationContext context = new StandardEvaluationContext();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        return expression.getValue(context, Object.class);
    }

    private static final String LOCK_PREFIX = "REDISSON_LOCK:";
}
