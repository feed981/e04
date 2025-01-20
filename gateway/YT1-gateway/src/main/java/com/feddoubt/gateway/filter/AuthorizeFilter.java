package com.feddoubt.gateway.filter;

import com.feddoubt.gateway.config.ConfigProperties2;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Slf4j
@Component
public class AuthorizeFilter implements Ordered, GlobalFilter {

    private final ConfigProperties2 configProperties;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";

    public AuthorizeFilter(ConfigProperties2 configProperties) {
        this.configProperties = configProperties;
    }

    private Key key;

    @PostConstruct
    public void init() {
        this.key = configProperties.getBase64Secret();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain ) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();


        if (request.getURI().getPath().contains("/api/v1/yt1")) {
            if (request.getURI().getPath().contains("/generate-token")) {
                // 放行
                return chain.filter(exchange);
            }

            // 獲取 Authorization header
            String token = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
            log.info("token:{}",token);
            // 驗證 token 是否存在
            if (token == null || !token.startsWith(BEARER_PREFIX)) {
                return handleError(exchange, "Missing or invalid token", HttpStatus.UNAUTHORIZED);
            }

            // 驗證 JWT
            try {
                String jwt = token.substring(BEARER_PREFIX.length());
                Claims claims = Jwts.parser()
                        .setSigningKey(key)
                        .parseClaimsJws(jwt)
                        .getBody();

                String userId = claims.getSubject();
                log.info("userId:{}",userId);

                // 添加到 header 傳遞給下游服務
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(USER_ID_HEADER, userId)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                return handleError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }
        }

        // 其他情况，返回禁止访问的响应
        response.setStatusCode(HttpStatus.FORBIDDEN);
        DataBuffer buffer = response.bufferFactory()
                .wrap("Access Denied".getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Mono<Void> handleError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
