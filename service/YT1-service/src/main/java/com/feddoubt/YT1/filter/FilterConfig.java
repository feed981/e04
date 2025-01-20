package com.feddoubt.YT1.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {
    
    @Bean
    public FilterRegistrationBean<ServiceFilter> userTrackingFilter(ServiceFilter filter) {
        FilterRegistrationBean<ServiceFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/v1/yt1/*");  // 設置要攔截的 URL pattern
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
}