package com.feddoubt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf().disable() // 关闭 CSRF 防护
            .authorizeExchange()
                // 放行指定路径
            .pathMatchers("/yt1/api/v1/yt1/**").permitAll()
            .pathMatchers("/yt1/api/v1/auth/token").permitAll()
            .pathMatchers("/ws/**").permitAll()
            .anyExchange().authenticated() // 其他路径需要认证
            .and()
            .headers().frameOptions().disable(); // 允许 WebSocket 的连接升级请求

        return http.build();
    }
}
