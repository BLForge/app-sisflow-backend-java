package io.snortexware.sisflow.auth.interfaces.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String ACCESS_COOKIE = "sisflow_access";
    public static final String REFRESH_COOKIE = "sisflow_refresh";

    private final boolean secure;
    private final String sameSite;

    public AuthCookieService(
            @Value("${auth.cookie.secure:false}") boolean secure,
            @Value("${auth.cookie.same-site:Lax}") String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public String accessCookie(String token, Duration maxAge) {
        return buildCookie(ACCESS_COOKIE, token, maxAge).toString();
    }

    public String refreshCookie(String token, Duration maxAge) {
        return buildCookie(REFRESH_COOKIE, token, maxAge).toString();
    }

    public String clearAccessCookie() {
        return buildCookie(ACCESS_COOKIE, "", Duration.ZERO).toString();
    }

    public String clearRefreshCookie() {
        return buildCookie(REFRESH_COOKIE, "", Duration.ZERO).toString();
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return resolveCookie(request, REFRESH_COOKIE);
    }

    public String resolveAccessToken(HttpServletRequest request) {
        return resolveCookie(request, ACCESS_COOKIE);
    }

    public void addAuthCookies(HttpHeaders headers, String accessToken, Duration accessMaxAge, String refreshToken, Duration refreshMaxAge) {
        headers.add(HttpHeaders.SET_COOKIE, accessCookie(accessToken, accessMaxAge));
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken, refreshMaxAge));
    }

    public void clearAuthCookies(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, clearAccessCookie());
        headers.add(HttpHeaders.SET_COOKIE, clearRefreshCookie());
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String resolveCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }

        for (var cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
