package io.snortexware.sisflow.config;

import io.snortexware.sisflow.security.ApiRateLimitFilter;
import io.snortexware.sisflow.security.IdempotencyFilter;
import io.snortexware.sisflow.security.JwtAuthFilter;
import io.snortexware.sisflow.security.LocalhostOnlyFilter;
import io.snortexware.sisflow.security.TenantIsolationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<JwtAuthFilter> disableJwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<LocalhostOnlyFilter> disableLocalhostOnlyFilterRegistration(LocalhostOnlyFilter filter) {
        FilterRegistrationBean<LocalhostOnlyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<TenantIsolationFilter> disableTenantIsolationFilterRegistration(TenantIsolationFilter filter) {
        FilterRegistrationBean<TenantIsolationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<IdempotencyFilter> disableIdempotencyFilterRegistration(IdempotencyFilter filter) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<ApiRateLimitFilter> disableApiRateLimitFilterRegistration(ApiRateLimitFilter filter) {
        FilterRegistrationBean<ApiRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
