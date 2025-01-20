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
public class ServiceUserFilter extends OncePerRequestFilter {

    private RabbitTemplate rabbitTemplate;

    public ServiceUserFilter(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().contains("/api/v1/yt1/generate-token")) {
            // 放行
            chain.doFilter(request, response);
        }

        String userId = request.getHeader("X-User-Id");
        log.info("userId:{}",userId);
        UserContext.setUserId(userId);
        rabbitTemplate.convertAndSend("userLogQueue",request);

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}