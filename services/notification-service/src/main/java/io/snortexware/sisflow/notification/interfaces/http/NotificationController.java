package io.snortexware.sisflow.notification.interfaces.http;

import io.snortexware.sisflow.notification.infrastructure.security.JwtService;
import io.snortexware.sisflow.notification.infrastructure.sse.NotificationPublisher;
import io.snortexware.sisflow.notification.infrastructure.sse.NotificationRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final JwtService jwtService;
    private final NotificationRegistry notificationRegistry;
    private final NotificationPublisher notificationPublisher;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            HttpServletRequest request
    ) {
        UUID userId = jwtService.extractUserId(resolveToken(authorizationHeader, request));
        SseEmitter emitter = notificationRegistry.add(userId, new SseEmitter(0L));

        try {
            notificationPublisher.sendConnected(emitter, userId);
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to initialize SSE stream", exception);
        }

        return emitter;
    }

    private String resolveToken(String authorizationHeader, HttpServletRequest request) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("sisflow_access".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing access token");
    }
}
