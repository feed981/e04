package com.feddoubt.YT1.filter;

import com.feddoubt.YT1.service.IpService;
import com.feddoubt.model.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class ServiceFilter extends OncePerRequestFilter {
    
    private final RabbitTemplate rabbitTemplate;
    private final IpService ipService;

    public ServiceFilter(RabbitTemplate rabbitTemplate ,IpService ipService) {
        this.rabbitTemplate = rabbitTemplate;
        this.ipService = ipService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        log.info("====== Filter Start ======");
        log.info("Request URI: {}", path);
        log.info("Request Method: {}", method);
        log.info("Request Headers: {}", Collections.list(request.getHeaderNames()));

        try {
            String userId = request.getHeader("X-User-Id");
            log.info("Processing request for userId: {}", userId);
            UserContext.setUserId(userId);

            if(ipService.getClientIp(request) == null) {
                log.info("未取得公網IP...");
            }

            //RabbitTemplate 默认使用 SimpleMessageConverter，它能够处理基本类型和 Serializable 接口的对象。
            rabbitTemplate.convertAndSend("userLogQueue", userId);

            log.info("Before chain.doFilter");
            chain.doFilter(request, response);
            log.info("After chain.doFilter");

        } catch (Exception e) {
            log.error("Filter error", e);
            throw e;  // 重要：确保异常被传播
        } finally {
            log.info("离开 ServiceFilter - URL: {}", request.getRequestURI());
            // 清理 ThreadLocal
            UserContext.clear();
        }
    }

}