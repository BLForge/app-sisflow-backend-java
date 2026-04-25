package io.snortexware.sisflow.config;

import io.snortexware.sisflow.security.RLSContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering interceptors and other web-related beans.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RLSContextInterceptor rlsContextInterceptor;

    public WebConfig(RLSContextInterceptor rlsContextInterceptor) {
        this.rlsContextInterceptor = rlsContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rlsContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/health",
                        "/files/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/github/webhook",
                        "/error"
                );
    }
}
