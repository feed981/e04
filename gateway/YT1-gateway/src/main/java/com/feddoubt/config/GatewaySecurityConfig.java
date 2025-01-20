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
            .pathMatchers("/api/v1/auth/token", "/ws/**").permitAll() // 放行指定路径
            .anyExchange().authenticated() // 其他路径需要认证
            .and()
            .headers().frameOptions().disable(); // 允许 WebSocket 的连接升级请求

        return http.build();
    }
}
