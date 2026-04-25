package io.snortexware.sisflow.security.exceptions;

public class InvalidRoleException extends RuntimeException {
    private final String code;

    public InvalidRoleException(String message) {
        super(message);
        this.code = "INVALID_ROLE";
    }

    public InvalidRoleException(String message, String code) {
        super(message);
        this.code = code;
    }

    public InvalidRoleException(String message, Throwable cause) {
        super(message, cause);
        this.code = "INVALID_ROLE";
    }

    public String getCode() {
        return code;
    }
}
