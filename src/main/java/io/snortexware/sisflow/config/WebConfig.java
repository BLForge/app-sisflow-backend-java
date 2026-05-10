package io.snortexware.sisflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.snortexware.sisflow.security.RLSContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rlsContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/github/webhook",
                        "/error"
                );
    }
}
