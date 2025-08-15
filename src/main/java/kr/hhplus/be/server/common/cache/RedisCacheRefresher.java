package kr.hhplus.be.server.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class RedisCacheRefresher {

    /**
     * 인기 상품 캐시 자동 갱신
     * 매 시간마다 캐시를 무효화하여 최신 데이터로 갱신되도록 함
     */
    @CacheEvict(value = "popularProducts", allEntries = true)
    @Scheduled(cron = "0 0 */1 * * *")  // 매 시간마다 실행 (초 분 시 일 월 요일)
    public void refreshPopularProductsCache() {
        log.info("인기 상품 캐시 자동 갱신: popularProducts 캐시가 무효화되었습니다. - {}", LocalDateTime.now());
    }

    /**
     * 상품 상세 정보 캐시 정리
     * 매일 새벽 2시에 오래된 캐시 정리
     */
    @CacheEvict(value = "productDetails", allEntries = true)
    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시 실행
    public void cleanupProductDetailsCache() {
        log.info("상품 상세 정보 캐시 정리: productDetails 캐시가 무효화되었습니다. - {}", LocalDateTime.now());
    }

    /**
     * 전체 캐시 정리 (주간 정리)
     * 매주 일요일 새벽 3시에 모든 캐시 정리
     */
    @CacheEvict(value = {"popularProducts", "productDetails"}, allEntries = true)
    @Scheduled(cron = "0 0 3 * * SUN")  // 매주 일요일 새벽 3시
    public void weeklyCleanupAllCache() {
        log.info("주간 전체 캐시 정리: 모든 캐시가 무효화되었습니다. - {}", LocalDateTime.now());
    }
}