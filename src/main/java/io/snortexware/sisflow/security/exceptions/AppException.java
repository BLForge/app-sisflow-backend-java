package io.snortexware.sisflow.security.exceptions;

import io.snortexware.sisflow.security.ErrorCode;
import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public AppException(ErrorCode code, HttpStatus status) {
        super(code.name());
        this.code = code;
        this.status = status;
    }

    public static AppException notFound()          { return new AppException(ErrorCode.NOT_FOUND,            HttpStatus.NOT_FOUND); }
    public static AppException forbidden()         { return new AppException(ErrorCode.FORBIDDEN,            HttpStatus.FORBIDDEN); }
    public static AppException unauthorized()      { return new AppException(ErrorCode.UNAUTHORIZED,         HttpStatus.UNAUTHORIZED); }
    public static AppException conflict()          { return new AppException(ErrorCode.CONFLICT,             HttpStatus.CONFLICT); }
    public static AppException badRequest()        { return new AppException(ErrorCode.BAD_REQUEST,          HttpStatus.BAD_REQUEST); }
    public static AppException fileTypeNotAllowed(){ return new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED,HttpStatus.BAD_REQUEST); }

    public ErrorCode getCode()   { return code; }
    public HttpStatus getStatus() { return status; }
}
