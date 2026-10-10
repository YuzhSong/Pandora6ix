package com.pandora6ix.backend.config;

import com.pandora6ix.backend.api.error.RequestIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }
}
