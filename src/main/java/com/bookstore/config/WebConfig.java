package com.bookstore.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // Login protection
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/buy/**",
                        "/orders"
                )
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/register"
                );

        // Admin protection
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns(
                        "/admin/**",
                        "/add",
                        "/save",
                        "/edit/**",
                        "/update",
                        "/delete/**"
                );
    }
}