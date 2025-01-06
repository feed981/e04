package com.feddoubt.common.YT1.bucket4j;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Bucket4jConfig {

    @Bean
    public Bucket downloadLimiterBucket() {
        // 配置令牌桶：每分钟最多允许 10 次下载，每次补充 1 个令牌
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(1, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Bean
    public DownloadLimiter downloadLimiter(Bucket bucket) {
        // 将 Bucket 注入到 DownloadLimiter 中
        return new DownloadLimiter(bucket);
    }
}
