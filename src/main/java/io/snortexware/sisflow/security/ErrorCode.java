package io.snortexware.sisflow.security;

/**
 * Opaque error codes returned to clients.
 * Never expose SQL errors, stack traces, internal messages, or path details.
 * Log the real cause server-side; send only the code to the client.
 */
public enum ErrorCode {

    // Auth
    AUTH_INVALID_CREDENTIALS,
    AUTH_EMAIL_NOT_CONFIRMED,
    AUTH_TOKEN_INVALID,
    AUTH_TOKEN_EXPIRED,
    AUTH_TENANT_MISMATCH,

    // Access control
    FORBIDDEN,
    UNAUTHORIZED,

    // Resource
    NOT_FOUND,
    CONFLICT,

    // Input
    VALIDATION_ERROR,
    BAD_REQUEST,

    // File
    FILE_TYPE_NOT_ALLOWED,
    FILE_TOO_LARGE,

    // Generic — never expose internals
    INTERNAL_ERROR;

    /**
     * Map a ResponseStatusException reason string to an opaque ErrorCode.
     * All unrecognised reasons fall back to a generic code based on HTTP status.
     */
    public static ErrorCode fromReason(String reason, int httpStatus) {
        if (reason == null) return fromStatus(httpStatus);
        String r = reason.toLowerCase();

        if (r.contains("credenciais") || r.contains("credentials")) return AUTH_INVALID_CREDENTIALS;
        if (r.contains("e-mail não confirmado") || r.contains("email not confirmed")) return AUTH_EMAIL_NOT_CONFIRMED;
        if (r.contains("token inválido") || r.contains("token invalid") || r.contains("token expired")
                || r.contains("expirado")) return AUTH_TOKEN_INVALID;
        if (r.contains("tenant mismatch")) return AUTH_TENANT_MISMATCH;
        if (r.contains("only image")) return FILE_TYPE_NOT_ALLOWED;
        if (r.contains("conflict") || r.contains("já cadastrado") || r.contains("já está")) return CONFLICT;
        if (r.contains("not found") || r.contains("não encontrado")) return NOT_FOUND;
        if (r.contains("insufficient permissions") || r.contains("forbidden") || r.contains("access denied")
                || r.contains("only") || r.contains("cannot")) return FORBIDDEN;

        return fromStatus(httpStatus);
    }

    private static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 409 -> CONFLICT;
            default -> INTERNAL_ERROR;
        };
    }
}
