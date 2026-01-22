package com.imyme.mine.global.error;

import lombok.Getter;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> meta;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.meta = Map.of();
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> meta) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.meta = Map.copyOf(meta);
    }
}