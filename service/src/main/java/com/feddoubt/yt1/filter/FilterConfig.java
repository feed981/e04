package com.feddoubt.YT1.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    // 配置和注册自定义过滤器，用于拦截和处理特定路径的请求
    @Bean
    public FilterRegistrationBean<ServiceFilter> userTrackingFilter(ServiceFilter filter) {
        FilterRegistrationBean<ServiceFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/v1/yt1/*");  // 先只用最简单的一层匹配
//        registrationBean.addUrlPatterns("/api/v1/yt1/**");
//        registrationBean.addUrlPatterns("/api/v1/yt1/*", "/api/v1/yt1/**");
//        registrationBean.addUrlPatterns("/api/v1/yt1/*", "/api/v1/yt1/*/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}