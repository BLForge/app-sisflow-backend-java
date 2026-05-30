package io.snortexware.sisflow.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final LocalhostOnlyFilter localhostOnlyFilter;
    private final TenantIsolationFilter tenantIsolationFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final IdempotencyFilter idempotencyFilter;

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, LocalhostOnlyFilter localhostOnlyFilter,
                         TenantIsolationFilter tenantIsolationFilter, ApiRateLimitFilter apiRateLimitFilter,
                         IdempotencyFilter idempotencyFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.localhostOnlyFilter = localhostOnlyFilter;
        this.tenantIsolationFilter = tenantIsolationFilter;
        this.apiRateLimitFilter = apiRateLimitFilter;
        this.idempotencyFilter = idempotencyFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("${cors.allowed.origin-patterns:}")
    private String allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; img-src 'self' data: https:; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"))
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> {})
                        .contentTypeOptions(cto -> {})
                )
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.Arrays.asList(allowedOrigins.split(",")));
                    if (!allowedOriginPatterns.isBlank())
                        config.setAllowedOriginPatterns(java.util.Arrays.asList(allowedOriginPatterns.split(",")));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("X-Forwarded-Host", "Authorization", "Content-Type", "X-Requested-With", "Idempotency-Key"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                GlobalExceptionHandler.writeError(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                GlobalExceptionHandler.writeError(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN))
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/health", "/tenants/register", "/tenants/branding", "/swagger-ui/**", "/v3/api-docs/**", "/github/webhook", "/internal/files/download/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(localhostOnlyFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantIsolationFilter, JwtAuthFilter.class)
                .addFilterAfter(idempotencyFilter, TenantIsolationFilter.class)
                .addFilterAfter(apiRateLimitFilter, IdempotencyFilter.class)
                .build();
    }
}
