package com.feddoubt.gateway.filter;

//import com.feddoubt.model.YT1.dtos.YT1Dto;
//import com.feddoubt.model.YT1.event.DownloadLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class AuthorizeFilter implements Ordered, GlobalFilter {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        if (request.getURI().getPath().contains("/api/v1/YT1")) {
            return chain.filter(exchange);
//            return DataBufferUtils.join(request.getBody())
//                    .flatMap(dataBuffer -> {
//                        String body = mergeRequestBody(dataBuffer);
//                        log.info("Request Body: {}", body);
//
//                        return parseDto(body)
//                                .flatMap(dto -> {
//                                    // 發送消息到消息隊列
//                                    sendMessage(exchange, dto);
//                                    return Mono.empty();
//                                })
//                                .then(chain.filter(exchange));
//                    });
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


    private String mergeRequestBody(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String mergeRequestBody(List<DataBuffer> dataBuffers) {
        byte[] bodyBytes = dataBuffers.stream()
                .map(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(dataBuffer); // 确保释放
                    }
                })
                .reduce(new byte[0], (a, b) -> {
                    byte[] result = new byte[a.length + b.length];
                    System.arraycopy(a, 0, result, 0, a.length);
                    System.arraycopy(b, 0, result, a.length, b.length);
                    return result;
                });

        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

//    private void sendMessage(ServerWebExchange exchange, YT1Dto dto) {
//        String ipAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
//        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
//
//        DownloadLogEvent event = new DownloadLogEvent(ipAddress, dto.getUrl(), dto.getFormat(), userAgent);
//        log.info("Sending event: {}", event);
//        rabbitTemplate.convertAndSend("downloadLogQueue", event);
//    }
//
//    private Mono<YT1Dto> parseDto(String body) {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            return Mono.just(mapper.readValue(body, YT1Dto.class));
//        } catch (JsonProcessingException e) {
//            log.error("Failed to parse DTO: {}", e.getMessage());
//            return Mono.error(e);
//        }
//    }

    private Mono<Void> handleErrorResponse(ServerWebExchange exchange, Throwable e) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(("Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
