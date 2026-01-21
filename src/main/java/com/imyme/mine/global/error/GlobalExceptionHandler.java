package com.imyme.mine.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(
            errorCode.getMessage(),
            errorCode.name(),
            e.getMeta()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorResponse.FieldError fieldError = ErrorResponse.FieldError.builder()
            .field(e.getName())
            .reason("type_mismatch")
            .value(e.getValue())
            .build();

        ErrorResponse response = ErrorResponse.of(
            ErrorCode.INVALID_REQUEST.getMessage(),
            ErrorCode.INVALID_REQUEST.name(),
            List.of(fieldError)
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse response = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
            ErrorCode.INTERNAL_SERVER_ERROR.name()
        );
        return ResponseEntity.internalServerError().body(response);
    }
}