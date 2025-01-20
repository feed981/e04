package com.feddoubt.YT1.filter;

import com.feddoubt.model.YT1.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class ServiceFilter extends OncePerRequestFilter {
    
    private final RabbitTemplate rabbitTemplate;
    
    public ServiceFilter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("Processing request for userId: {}", userId);
            
            // 設置到 ThreadLocal
            UserContext.setUserId(userId);
            
            // 發送到 RabbitMQ
            rabbitTemplate.convertAndSend("userLogQueue", request);
            
            chain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal
            UserContext.clear();
        }
    }

}