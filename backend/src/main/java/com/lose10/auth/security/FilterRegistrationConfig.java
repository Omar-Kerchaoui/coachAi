package com.lose10.auth.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-registers every {@code Filter} bean into the servlet
 * container's own filter chain, in addition to whatever Spring Security
 * does with it via {@code HttpSecurity.addFilterBefore}. Without this,
 * JwtAuthenticationFilter would run twice per request. Disabling the
 * generic servlet registration leaves Spring Security's chain (configured
 * in {@link SecurityConfig}) as the only place it runs.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
