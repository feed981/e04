package com.feddoubt.gateway.filter;

import com.feddoubt.common.YT1.config.jwt.JwtProvider;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class AuthorizeFilter implements Ordered, GlobalFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final JwtProvider jwtProvider;

    public AuthorizeFilter(JwtProvider jwtProvider){
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain ) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        if (request.getURI().getPath().contains("/api/v1/auth/token")) {
            log.info("AuthorizeFilter");
            return chain.filter(exchange);  // 放行取得 token 的請求
        }

        if (request.getURI().getPath().contains("/api/v1/yt1")) {
            String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
            String userId;

            // 如果沒有 token，生成新的
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                userId = UUID.randomUUID().toString();
                String newToken = jwtProvider.generateToken(userId);

                // 將新 token 加入 response header
                response.getHeaders().add(AUTHORIZATION_HEADER, BEARER_PREFIX + newToken);

                // 修改 request，加入 user id
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(USER_ID_HEADER, userId)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }

            try {
                // 驗證既有 token
                userId = jwtProvider.extractUsername(authHeader);
                log.info("userId: {}", userId);

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
        return -1;
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
