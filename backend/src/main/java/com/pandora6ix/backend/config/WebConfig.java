package com.pandora6ix.backend.config;

import com.pandora6ix.backend.api.error.RequestIdFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig
        implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
    private final BusinessAuthentication authentication;

    public WebConfig(BusinessAuthentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public void addInterceptors(
            org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authentication).addPathPatterns("/api/v1/**");
    }

    @Bean
    RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }
}
