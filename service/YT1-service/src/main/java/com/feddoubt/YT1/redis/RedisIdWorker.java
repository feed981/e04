package com.feddoubt.YT1.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RedisIdWorker {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final long BEGIN_TIMESTAMP = 1704067200L; // 初始时间因为是固定的，所以放在常量
    private static final int COUNT_BITS = 32; // 序列号的位数
    public long nextId(String keyPrefix){

//        符号位: 1bit,永远为0
//        时间戳: 31bit,以秒为单位,可以使用69年
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

//        序列号: 32bit,秒内的计数器,支持每秒产生2^32个不同ID
        // 获取当前日期 精确到天
        String yyyyMMdd = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 自增
        long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + yyyyMMdd);
        log.info("increment:{}", increment);
        log.info("timestamp:{}", timestamp);
        // 先左移再填充
        return timestamp << COUNT_BITS | increment;
    }
}
