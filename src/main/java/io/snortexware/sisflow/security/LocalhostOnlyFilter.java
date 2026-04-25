package io.snortexware.sisflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LocalhostOnlyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isSwagger = path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");

        if (isSwagger) {
            String remoteAddr = request.getRemoteAddr();
            boolean isLocal = remoteAddr.equals("127.0.0.1")
                    || remoteAddr.equals("0:0:0:0:0:0:0:1")
                    || remoteAddr.equals("::1");

            if (!isLocal) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
