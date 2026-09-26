package com.nexters.sseotdabwa.common.config;

import com.nexters.sseotdabwa.common.security.NicknameRequiredInterceptor;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final NicknameRequiredInterceptor nicknameRequiredInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(nicknameRequiredInterceptor);
    }
}
