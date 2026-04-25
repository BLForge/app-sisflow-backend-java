package io.snortexware.sisflow.security.exceptions;

public class UnauthorizedException extends RuntimeException {
    private final String code;

    public UnauthorizedException(String message) {
        super(message);
        this.code = "UNAUTHORIZED";
    }

    public UnauthorizedException(String message, String code) {
        super(message);
        this.code = code;
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
        this.code = "UNAUTHORIZED";
    }

    public String getCode() {
        return code;
    }
}
