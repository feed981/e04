package com.feddoubt.yt1.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DownloadLimiter {

    private static final long DUPLICATE_REQUEST_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30); // 30秒

    private final StringRedisTemplate stringRedisTemplate;

    public DownloadLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryDownload(String requestHash) {
        long now = System.currentTimeMillis();
        // 尝试获取请求时间，同时设置新的TTL为30秒
        Boolean isSet = stringRedisTemplate.opsForValue().setIfAbsent(requestHash, String.valueOf(now),
                DUPLICATE_REQUEST_INTERVAL_MS, TimeUnit.MILLISECONDS);
        // 如果 Redis 中已存在相同键（且未过期），则请求重复。如果键不存在，则记录请求并允许通过
        return !Boolean.FALSE.equals(isSet);
    }
}
