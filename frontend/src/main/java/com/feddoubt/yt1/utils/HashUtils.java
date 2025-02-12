package com.feddoubt.yt1.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@Component
public class HashUtils {
    public String generateRequestHash(String... components) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 将所有组件用 ":" 连接，生成单一字符串
            String data = String.join(":", components);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to generate hash", e);
        }
    }
}
