package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.config.ConfigProperties;
import com.feddoubt.model.YT1.context.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/yt1")
public class TokenController {

    private final ConfigProperties configProperties;
    private RabbitTemplate rabbitTemplate;

    public TokenController(ConfigProperties configProperties ,RabbitTemplate rabbitTemplate) {
        this.configProperties = configProperties;
        this.rabbitTemplate = rabbitTemplate;
    }
    private Key key;
    @PostConstruct
    public void init() {
        this.key = configProperties.getBase64Secret();
    }

    @PostMapping("/generate-token")
    public String generateToken(HttpServletRequest request) {
        String userId = UUID.randomUUID().toString();
        UserContext.setUserId(userId);
        rabbitTemplate.convertAndSend("userLogQueue",request);

        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }
    
    @PostMapping("/verify-token")
    public boolean verifyToken(@RequestHeader("Authorization") String token) {
        try {
            Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token.replace("Bearer ", ""));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}