package io.snortexware.sisflow.security;

import io.snortexware.sisflow.security.exceptions.AccessDeniedException;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.security.exceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

/**
 * Returns only { code, status } to the client — never internal messages,
 * stack traces, SQL errors, paths, or permission names.
 * Everything is logged server-side for debugging.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleApp(AppException ex) {
        log.warn("AppException [{}]: {}", ex.getStatus().value(), ex.getCode());
        return error(ex.getStatus(), ex.getCode());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // Log field details internally, never expose them
        log.warn("Validation failed: {}", ex.getBindingResult().getAllErrors());
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        ErrorCode code = ErrorCode.fromReason(ex.getReason(), status);
        // Log the real reason internally
        log.warn("ResponseStatusException [{}] {}: {}", status, code, ex.getReason());
        return error(HttpStatus.valueOf(status), code);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Log full stack trace internally, never expose it
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, ErrorCode code) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "code", code.name()
        ));
    }

    /** For use in servlet filters where ResponseEntity is not available. */
    public static void writeError(HttpServletResponse response, HttpStatus status, ErrorCode code) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status.value() + ",\"code\":\"" + code.name() + "\"}");
    }
}
