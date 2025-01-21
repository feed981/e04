package com.feddoubt.YT1.filter;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    // 配置访问控制策略，定义哪些路径需要认证，哪些路径不需要认证
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()  // 虽然使用 Nginx，但先禁用来排除干扰
                .authorizeRequests()
                .antMatchers("/api/v1/yt1/**").permitAll()
                .antMatchers("/api/v1/auth/token").permitAll()
                .antMatchers("/ws/**").permitAll()
                .anyRequest().authenticated();
    }
}
