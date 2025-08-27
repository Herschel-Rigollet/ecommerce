package kr.hhplus.be.server.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redisson")
@Data
public class RedissonProperties {

    private String address = "redis://localhost:6379";
    private int connectionMinimumIdleSize = 10;
    private int connectionPoolSize = 64;
    private int idleConnectionTimeout = 10000;
    private int connectTimeout = 10000;
    private int timeout = 3000;
    private int retryAttempts = 3;
    private int retryInterval = 1500;
}
