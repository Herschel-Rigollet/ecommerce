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
public class DistributedLockAspect {
    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock redissonLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String lockKey = LOCK_PREFIX + getDynamicValue(signature.getParameterNames(),
                joinPoint.getArgs(),
                redissonLock.key());

        RLock rLock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 시도
            boolean available = rLock.tryLock(redissonLock.waitTime(),
                    redissonLock.leaseTime(),
                    redissonLock.timeUnit());

            if (!available) {
                log.warn("락 획득 실패: {}", lockKey);
                throw new DistributedLockException(redissonLock.failMessage());
            }

            log.info("분산락 획득 성공: {} (스레드: {})", lockKey, Thread.currentThread().getName());

            // 2. 트랜잭션 시작 -> 비즈니스 로직 실행 -> 트랜잭션 종료
            return aopForTransaction.proceed(joinPoint);

        } catch (InterruptedException e) {
            log.error("락 획득 중 인터럽트 발생: {}", lockKey, e);
            throw new DistributedLockException("락 획득 중 인터럽트가 발생했습니다.", e);
        } finally {
            // 3. 락 해제 (현재 스레드가 보유한 락만 해제)
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
