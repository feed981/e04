package com.feddoubt.common.YT1.bucket4j;

import io.github.bucket4j.Bucket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DownloadLimiter {

    private final Bucket bucket;
    private final ConcurrentHashMap<String, Long> requestCache = new ConcurrentHashMap<>();

    private static final long DUPLICATE_REQUEST_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30); // 30秒

    public DownloadLimiter(Bucket bucket) {
        this.bucket = bucket;
    }

    public boolean tryDownload() {
        return bucket.tryConsume(1);
    }

    public boolean tryDownload(String requestHash) {
        long now = System.currentTimeMillis();
        Long lastRequestTime = requestCache.get(requestHash);

        if (lastRequestTime != null && (now - lastRequestTime) < DUPLICATE_REQUEST_INTERVAL_MS) {
            return false; // 判定为重复请求
        }
//        String lastRequestTimeStr = redisTemplate.opsForValue().get(requestHash);// 从 Redis 获取上一次请求时间

//        if (lastRequestTimeStr != null) {
//            long lastRequestTime = Long.parseLong(lastRequestTimeStr);
//            if ((now - lastRequestTime) < DUPLICATE_REQUEST_INTERVAL_MS) {
//                return false; // 判定为重复请求
//            }
//        }


        if (bucket.tryConsume(1)) {
            requestCache.put(requestHash, now); // 更新请求时间戳

            // 请求允许，将当前时间存储到 Redis，并设置过期时间
//            redisTemplate.opsForValue().set(requestHash, String.valueOf(now),
//                    DUPLICATE_REQUEST_INTERVAL_MS, TimeUnit.MILLISECONDS);
            return true;
        }

        return false; // 超过令牌桶限制
    }
}
