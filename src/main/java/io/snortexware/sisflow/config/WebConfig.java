package io.snortexware.sisflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.snortexware.sisflow.security.RLSContextInterceptor;
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
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
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
