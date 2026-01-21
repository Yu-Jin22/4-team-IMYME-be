package com.imyme.mine.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String message;
    private final ErrorData data;

    @Getter
    @Builder
    public static class ErrorData {
        private final String errorCode;
        @Builder.Default
        private final List<FieldError> errors = List.of();
        @Builder.Default
        private final Map<String, Object> meta = Map.of();
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String reason;
        private final Object value;
    }

    public static ErrorResponse of(String message, String errorCode) {
        return ErrorResponse.builder()
            .message(message)
            .data(ErrorData.builder()
                .errorCode(errorCode)
                .build())
            .build();
    }

    public static ErrorResponse of(String message, String errorCode, Map<String, Object> meta) {
        return ErrorResponse.builder()
            .message(message)
            .data(ErrorData.builder()
                .errorCode(errorCode)
                .meta(meta)
                .build())
            .build();
    }

    public static ErrorResponse of(String message, String errorCode, List<FieldError> errors) {
        return ErrorResponse.builder()
            .message(message)
            .data(ErrorData.builder()
                .errorCode(errorCode)
                .errors(errors)
                .build())
            .build();
    }
}