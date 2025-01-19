package com.feddoubt.YT1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feddoubt.common.YT1.config.message.CustomHttpStatus;
import com.feddoubt.common.YT1.config.message.ResponseUtils;
import com.feddoubt.model.YT1.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class IpGeolocationService {

    private final UserLogService userLogService;

    public IpGeolocationService(UserLogService userLogService) {
        this.userLogService = userLogService;
    }

    /**
     * {
     *     "ip": "114.136.204.88",
     *     "hostname": "114-136-204-88.emome-ip.hinet.net",
     *     "city": "Taipei",
     *     "region": "Taiwan",
     *     "country": "TW",
     *     "loc": "25.0531,121.5264", // 這裡包含了緯度和經度
     *     "org": "AS17421 Mobile Business Group",
     *     "timezone": "Asia/Taipei",
     *     "readme": "https://ipinfo.io/missingauth"
     * }
     *
     * loc 但 IP 地理定位不准确
     * IP 地理定位的精度通常受到多种因素的影响，包括网络结构、使用环境以及数据库的准确性。因此，出现 6 公里左右的误差并不罕见
     */
    private static final String ipinfoAPI_URL = "http://ipinfo.io/{ip}/json";

    // 經緯度
    public void getLocationByIp(UserLog userLog) {
        String ipAddress = userLog.getIpAddress();
        RestTemplate restTemplate = new RestTemplate();
        // 生成 URL 並查詢 API
        String url = UriComponentsBuilder.fromUriString(ipinfoAPI_URL)
                                            .buildAndExpand(ipAddress)
                                            .toUriString();
        // 發送請求，解析返回的 JSON
        String response = restTemplate.getForObject(url, String.class);

        if(response != null){
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                JsonNode jsonNode = objectMapper.readTree(response);
                log.info("response:{}",response);
                String loc = jsonNode.get("loc").asText();
                if(loc.contains(",")){
                    userLog.setLatitude(new BigDecimal(loc.split(",")[0]));
                    userLog.setLongitude(new BigDecimal(loc.split(",")[1]));
                    userLog.setCreatedAt(LocalDateTime.now());
                    userLog.setMethod(ipinfoAPI_URL);
                    userLogService.saveUserLog(userLog);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}