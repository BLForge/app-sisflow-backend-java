package io.snortexware.sisflow.security.exceptions;

public class InvalidPermissionException extends RuntimeException {
    private final String code;

    public InvalidPermissionException(String message) {
        super(message);
        this.code = "INVALID_PERMISSION";
    }

    public InvalidPermissionException(String message, String code) {
        super(message);
        this.code = code;
    }

    public InvalidPermissionException(String message, Throwable cause) {
        super(message, cause);
        this.code = "INVALID_PERMISSION";
    }

    public String getCode() {
        return code;
    }
}
