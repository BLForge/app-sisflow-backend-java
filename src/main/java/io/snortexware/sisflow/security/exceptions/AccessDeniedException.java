package io.snortexware.sisflow.security.exceptions;

public class AccessDeniedException extends RuntimeException {
    private final String code;
    private final String requiredPermission;

    public AccessDeniedException(String message) {
        super(message);
        this.code = "ACCESS_DENIED";
        this.requiredPermission = null;
    }

    public AccessDeniedException(String message, String requiredPermission) {
        super(message);
        this.code = "ACCESS_DENIED";
        this.requiredPermission = requiredPermission;
    }

    public AccessDeniedException(String message, String code, String requiredPermission) {
        super(message);
        this.code = code;
        this.requiredPermission = requiredPermission;
    }

    public String getCode() {
        return code;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }
}
