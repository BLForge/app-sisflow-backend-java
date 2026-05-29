package io.snortexware.sisflow.security;

import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final TenantContext tenantContext;
    private final AuthorizationService authorizationService;

    @Value("${security.api-rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${security.api-rate-limit.user.read.client:600}")
    private long clientReadPerMinute;
    @Value("${security.api-rate-limit.user.write.client:120}")
    private long clientWritePerMinute;
    @Value("${security.api-rate-limit.user.read.developer:900}")
    private long developerReadPerMinute;
    @Value("${security.api-rate-limit.user.write.developer:180}")
    private long developerWritePerMinute;
    @Value("${security.api-rate-limit.user.read.moderator:1200}")
    private long moderatorReadPerMinute;
    @Value("${security.api-rate-limit.user.write.moderator:240}")
    private long moderatorWritePerMinute;
    @Value("${security.api-rate-limit.user.read.admin:1800}")
    private long adminReadPerMinute;
    @Value("${security.api-rate-limit.user.write.admin:360}")
    private long adminWritePerMinute;

    @Value("${security.api-rate-limit.tenant.read:3000}")
    private long tenantReadPerMinute;
    @Value("${security.api-rate-limit.tenant.write:900}")
    private long tenantWritePerMinute;

    @Value("${security.api-rate-limit.api-key.read:1200}")
    private long apiKeyReadPerMinute;
    @Value("${security.api-rate-limit.api-key.write:300}")
    private long apiKeyWritePerMinute;

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/auth/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    private static final List<String> PUBLIC_PATHS = List.of(
            "/health",
            "/github/webhook",
            "/tenants/register",
            "/tenants/branding"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean readRequest = isReadRequest(request.getMethod());
        UUID userId = tenantContext.getCurrentUser();
        UUID tenantId = tenantContext.getCurrentTenant();
        String apiKey = request.getHeader("X-API-Key");

        if (userId != null) {
            long userLimit = resolveUserLimit(userId, readRequest);
            if (!consumeOrReject("user:" + userId, userLimit, response, "user")) {
                return;
            }
        }

        if (tenantId != null) {
            long tenantLimit = readRequest ? tenantReadPerMinute : tenantWritePerMinute;
            if (!consumeOrReject("tenant:" + tenantId, tenantLimit, response, "tenant")) {
                return;
            }
        }

        if (apiKey != null && !apiKey.isBlank()) {
            long apiKeyLimit = readRequest ? apiKeyReadPerMinute : apiKeyWritePerMinute;
            if (!consumeOrReject("api-key:" + apiKey.trim(), apiKeyLimit, response, "api-key")) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean consumeOrReject(String subject, long limit, HttpServletResponse response, String dimension)
            throws IOException {
        var result = rateLimitService.consume("api:" + dimension + ":" + subject, limit, Duration.ofMinutes(1));
        if (!result.allowed()) {
            log.warn("API rate limit exceeded for {} [{}]", dimension, subject);
            GlobalExceptionHandler.writeError(response, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMIT_EXCEEDED);
            return false;
        }
        return true;
    }

    private long resolveUserLimit(UUID userId, boolean readRequest) {
        if (authorizationService.isAdminOrAbove(userId)) {
            return readRequest ? adminReadPerMinute : adminWritePerMinute;
        }
        if (authorizationService.isModeratorOrAbove(userId)) {
            return readRequest ? moderatorReadPerMinute : moderatorWritePerMinute;
        }
        if (authorizationService.isDeveloperOrAbove(userId)) {
            return readRequest ? developerReadPerMinute : developerWritePerMinute;
        }
        return readRequest ? clientReadPerMinute : clientWritePerMinute;
    }

    private boolean isReadRequest(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
