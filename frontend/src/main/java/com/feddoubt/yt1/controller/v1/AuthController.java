package com.feddoubt.yt1.controller.v1;

import com.feddoubt.common.config.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final JwtProvider jwtProvider;

    public AuthController(JwtProvider jwtProvider){
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/token")
    public ResponseEntity<String> generateToken() {
        log.info("AuthController");
        String userId = UUID.randomUUID().toString();
        String token = jwtProvider.generateToken(userId);
        return ResponseEntity.ok()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .body(token);
    }
}