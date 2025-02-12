package com.feddoubt.YT1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feddoubt.YT1.redis.RedisConstants;
import com.feddoubt.YT1.redis.RedisIdWorker;
import com.feddoubt.model.context.UserContext;
import com.feddoubt.model.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class IpService {

    private final UserLogService userLogService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisIdWorker redisIdWorker;
    private static final long CACHE_TTL_MINUTES = 5; // 5 分鐘 TTL

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public IpService(UserLogService userLogService , StringRedisTemplate stringRedisTemplate , RedisIdWorker redisIdWorker) {
        this.userLogService = userLogService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisIdWorker = redisIdWorker;

        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    private static final String IPIFY_API_URL = "https://api.ipify.org?format=json";

    public String getClientIp(HttpServletRequest request) {
        String ip = getIpForUser();
        if(getIpForUser() != null){
            log.info("redis 已有外網...");
            return ip;
        }

        log.info("嘗試從 HTTP 頭部獲取真實的公網 IP...");
        ip = getIpFromRequest(request);
        log.info("簡單判斷 IP 是否屬於內網範圍...");
        if(!isInternalIp(ip)){
            log.info("不屬於內網...");
            return ip;
        }

        log.info("回退機制，使用外部 API: {} 來獲取公網 IP...",IPIFY_API_URL);
        return setIpForUser();
    }

    private String getIpFromRequest(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean isInternalIp(String ip) {
        // 簡單判斷內網 IP 的範例，可擴展為更多範圍檢查
        return ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.16.") || ip.startsWith("127.");
    }

    public String setIpForUser() {
        String userId = UserContext.getUserId();
        String redisKey = RedisConstants.USER_IP_KEY + userId;
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        // 如果沒有緩存，調用 API 獲取公網 IP
        String response = restTemplate.getForObject(IPIFY_API_URL, String.class);
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String ipAddress = jsonNode.get("ip").asText();

            // 將獲取的 IP 存入 Redis，設置 TTL
            valueOps.set(redisKey, ipAddress, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            return ipAddress;
        } catch (Exception e) {
            // 日誌記錄和錯誤處理
            e.printStackTrace();
            return null;
        }
    }

    public String getIpForUser() {
        String userId = UserContext.getUserId();
        String redisKey = RedisConstants.USER_IP_KEY + userId;
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        return valueOps.get(redisKey);
    }

    private static final String IPINFO_API_URL = "http://ipinfo.io/{ip}/json";

    public String getRedisLocation() {
        String userId = UserContext.getUserId();
        String key = RedisConstants.USERLOG_IP_KEY + userId;
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        return hashOps.get(key, "loc");
    }

    public void saveUserLog() {
        String userId = UserContext.getUserId();
        String ipForUser = getIpForUser();
        String key = RedisConstants.USERLOG_IP_KEY + userId;

        log.info("使用外部 API: {} 來獲取公網 IP loc...",IPINFO_API_URL);
        // 生成 URL 並查詢 API
        String url = UriComponentsBuilder.fromUriString(IPINFO_API_URL)
                .buildAndExpand(ipForUser)
                .toUriString();

        String response = restTemplate.getForObject(url, String.class);
        if(response == null){
            return;
        }
        log.info("response:{}",response);
        try {
            //redis
            log.info("key:{} ,IP loc存redis...",key);
            JsonNode jsonNode = objectMapper.readTree(response);
            String ip = jsonNode.get("ip").asText();
            String loc = jsonNode.get("loc").asText();
            HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
            hashOps.put(key, "ip", ip);
            hashOps.put(key, "loc", loc);
            setRedisTTL(key);
            //DB
            log.info("IP loc存db...");
            UserLog userLog = new UserLog();
            userLog.setIpAddress(ipForUser);
            userLog.setLoc(loc);
            userLog.setUid(redisIdWorker.nextId("location:" + ipForUser));
            userLog.setCreatedAt(LocalDateTime.now());;
            userLogService.saveUserLog(userLog);
        } catch (Exception e) {
            // 日志錯誤處理
            e.printStackTrace();
        }
    }

    private void setRedisTTL(String key) {
        long secondsToEndOfDay = Duration.between(LocalTime.now(), LocalTime.MAX).getSeconds();
        stringRedisTemplate.expire(key, Duration.ofSeconds(secondsToEndOfDay));
    }
}