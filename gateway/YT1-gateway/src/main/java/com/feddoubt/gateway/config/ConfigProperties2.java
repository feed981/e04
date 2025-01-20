package com.feddoubt.gateway.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;

@RefreshScope //实时刷新 Nacos 中更新的配置
@Component
public class ConfigProperties2 {

    @Value("${jwt.secret}")
    private String base64Secret;

    public Key getBase64Secret() {
        byte[] decodedKey = Base64.getDecoder().decode(base64Secret);
        return Keys.hmacShaKeyFor(decodedKey);
    }
}
