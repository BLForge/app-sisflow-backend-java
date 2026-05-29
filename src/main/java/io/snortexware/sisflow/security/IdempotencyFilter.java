package io.snortexware.sisflow.security;

import io.snortexware.sisflow.services.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> IDEMPOTENT_POST_PATTERNS = List.of(
            "/auth/register",
            "/tickets",
            "/tickets/*/transfer",
            "/tickets/*/interactions",
            "/tickets/*/homologations"
    );

    private final RateLimitService rateLimitService;
    private final TenantContext tenantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        if (!requiresIdempotencyHandling(method, request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = tenantContext.hasUser() ? tenantContext.getCurrentUser().toString() : "anonymous";
        String key = "idempotency:" + userId + ":" + idempotencyKey;

        var result = rateLimitService.consume(key, 1, Duration.ofMinutes(5));
        if (!result.allowed()) {
            log.warn("Duplicate request detected: {}", key);
            GlobalExceptionHandler.writeError(response, 
                    org.springframework.http.HttpStatus.CONFLICT, 
                    ErrorCode.DUPLICATE_REQUEST);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresIdempotencyHandling(String method, String path) {
        if (!"POST".equals(method)) {
            return false;
        }

        return IDEMPOTENT_POST_PATTERNS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
