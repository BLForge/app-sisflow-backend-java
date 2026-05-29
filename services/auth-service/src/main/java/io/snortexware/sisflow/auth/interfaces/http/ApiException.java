package io.snortexware.sisflow.auth.interfaces.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    public ApiException(HttpStatus status, ErrorCode code) {
        super(code.name());
        this.status = status;
        this.code = code;
    }

    public static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN); }
    public static ApiException unauthorized() { return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED); }
    public static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND); }
    public static ApiException badRequest() { return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST); }
    public static ApiException conflict() { return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT); }
}
