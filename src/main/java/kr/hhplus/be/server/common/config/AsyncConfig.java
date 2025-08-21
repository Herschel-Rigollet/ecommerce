package kr.hhplus.be.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 쿠폰 발급 전용 스레드 풀
     * 비동기 쿠폰 발급 처리를 위한 전용 스레드 풀
     */
    @Bean(name = "couponTaskExecutor")
    public Executor couponTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);        // 기본 스레드 수
        executor.setMaxPoolSize(20);         // 최대 스레드 수
        executor.setQueueCapacity(100);      // 대기 큐 크기
        executor.setThreadNamePrefix("Coupon-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}