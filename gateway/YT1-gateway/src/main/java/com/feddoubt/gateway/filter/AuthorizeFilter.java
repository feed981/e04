package com.feddoubt.gateway.filter;

import com.feddoubt.model.YT1.event.DownloadLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthorizeFilter implements Ordered, GlobalFilter {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request和response对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String ipAddress = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        String url = request.getQueryParams().getFirst("url");
        String format = request.getQueryParams().getFirst("format");
        String userAgent = request.getHeaders().getFirst("User-Agent");

        //2.判断是否是
        if(request.getURI().getPath().contains("/YT1")){
            // 構造事件
            DownloadLogEvent event = new DownloadLogEvent(ipAddress, url, format, userAgent);
            //convertQueue,download-log-queue
            rabbitTemplate.convertAndSend("downloadLogQueue", event);
            // 放行
            return chain.filter(exchange);
        }
        //6.放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
